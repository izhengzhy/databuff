<template>
  <div>
    <el-popover v-if='showGroup' v-model='roleGroupModel' placement="bottom" trigger="click" @hide='hideResetRoleIdHandle' popper-class="role-group-popover">
      <span class="user-group-trigger ell" slot='reference'>{{ $t('roleGroup.title', { name: getCurrGroupName || $t('common.empty') }) }}</span>

      <div class="role-group-wrapper flex-h">
        <div class="role-group-tree">
          <el-tree
            :data='roleOptions' default-expand-all
            :props="{ label: 'roleName' }" node-key='roleId'
            :current-node-key='choosedRoleId'
            :check-strictly='true' highlight-current :expand-on-click-node='false'
            @node-click="handleCheckChange" ref="roleTree">
          </el-tree>
        </div>
        <div class="role-group-list">
          <el-divider content-position="center" class="role-group-split">{{ $t('roleGroup.label') }}</el-divider>
          <el-radio-group v-model="roleForm.id" class="role-group-radio-group">
            <el-radio v-for='item in getRoleGroupList' :key='item.gid' :label='item.gid'
              class="role-group-radio">
              <span :title="item.gname">{{ item.gname }}</span>
            </el-radio>
          </el-radio-group>
          <div v-show="!getRoleGroupList.length" class="describe font-12 tc">{{ $t('common.noData') }}</div>
        </div>
        <div class="role-group-footer">
          <el-button @click="toggleAccountGroupHandle" :disabled='roleForm.id === getCurrGroupId || !roleGroupList.length' plain type="primary" size="mini">{{ $t('common.save') }}</el-button>
        </div>
      </div>
    </el-popover>

    <!-- 切换管理域提示框 -->
    <el-dialog :visible.sync='showToggleConfirmDialog' :title="$t('roleGroup.switchTitle')" width="400px" append-to-body modal-append-to-body
      :before-close="cancelUgHandle">
      <div>
        <div class="toggle-confirm-cont">
          <div>
            <i class="el-icon-warning toggle-confirm-icon mr-20"></i>
          </div>
          <div>{{ $t('roleGroup.switchConfirm') }}</div>
        </div>
        <div>
        </div>
        <div class="toggle-confirm-footer mt-15 flex-h-jc">
          <el-checkbox v-model="nextToggleShowTip" class="toggle-confirm-next-show">{{ $t('roleGroup.dontShowAgain') }}</el-checkbox>
          <div>
            <el-button size="small"  @click="cancelUgHandle">{{ $t('common.cancel') }}</el-button>
            <el-button size='small' type="primary"  @click="postUgHandle()">{{ $t('common.confirm') }}</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop, Watch } from 'vue-property-decorator';
import { namespace } from 'vuex-class';
import deepClone from 'lodash/cloneDeep';
import { Tree } from 'element-ui';
import { StringIsEmpty } from '@/utils/common';
import { getAgi } from '@/utils/jsCookie';

const UserModel = namespace('User');

@Component
export default class RoleGroup extends Vue {
  @UserModel.State private currGroup!: any[];
  @UserModel.State private prevGroup!: any[];
  @UserModel.State private roleList!: any[];
  @UserModel.State private userInfo!: any;
  @UserModel.Mutation('SET_CURRENT_GROUP') private setCurrentGroup!: (payload: any[]) => void;
  @UserModel.Mutation('SET_PREV_GROUP') private setPrevGroup!: (payload: any[]) => void;

  public $refs!: {
    roleTree: Tree
  }

  private groupEnabled = false;
  private showToggleConfirmDialog = false;
  private nextToggleShowTip = false;

  private roleForm = {
    id: '',
    name: '',
  }
  private roleGroupModel = false;
  private choosedRole: any = null;
  private roleOptions: any[] = [];
  private roleLoading = false;
  private choosedRoleId: number|null = null;
  private roleGroupList: any[] = [];

  get showGroup () {
    if (!this.groupEnabled || this.isLimitRole) {
      return false;
    }
    const currPath = this.$route.path
    const ignoreList = [
      '/appMonitor/trace',
      '/appMonitor/serviceFlow',
      '/config/ai',
      '/sysManage/role',
      '/sysManage/license',
      '/sysManage/notice',
      '/sysManage/basic',
      '/sysManage/systemEvent',
      '/sysManage/operationAudit',
      // '/configManage/alarm/ruleSetting',
      // '/configManage/alarm/responseSetting',
    ]
    return !ignoreList.includes(currPath);
  }

  get getCurrGroupId () {
    const localGroupId = getAgi();
    return isNaN(Number(localGroupId)) ? localGroupId : Number(localGroupId)
  }

  get getCurrRole () {
    return this.userInfo?.currentRole
  }

  get getCurrRoleId () {
    return this.userInfo?.currentRole?.roleId
  }

  get currRoleGroupRela () {
    return this.getCurrRole?.roleGroupRelations || [];
  }

  @Watch('currRoleGroupRela', { deep: true, immediate: true })
  private onCurrRoleChange () {
    this.roleGroupRefresh();
  }

  @Watch('roleList', { deep: true })
  private onRoleList () {
    this.roleGroupRefresh();
  }

  get getCurrGroupName () {
    return this.currGroup.map(g => g.gname).join(' + ')
  }

  get getRoleGroupList () {
    if (this.choosedRoleId === this.getCurrRoleId) {
      return [...this.roleGroupList]
    } else {
      const { roleGroupRelations = [] } = this.choosedRole || {};
      const ids = (roleGroupRelations || []).map((rela: any) => rela.gid);
      return this.roleGroupList.filter(g => ids.includes(g.gid));
    }
  }

  get isLimitRole () {
    return this.getCurrRoleId && this.getCurrRoleId <= 1
  }

  get getEnableStatus () {
    return this.$store.getters['User/getGroupEnabled']
  }

  @Watch('getEnableStatus', { immediate: true })
  private onGroupEnabled (newVal: boolean) {
    this.groupEnabled = newVal;
  }

  @Watch('showGroup', { immediate: true })
  private onShowGroupChange (newVal: boolean, oldVal?: boolean) {
    if (newVal) {
      this.restorePrevGroupHandle();
      return;
    }
    if (oldVal === undefined || oldVal) {
      this.suspendCurrentGroupHandle();
    }
  }

  private created() {
    this.choosedRoleId = Number(this.userInfo?.roleid);
    this.choosedRole = {...this.getCurrRole};
    const localGroupId = getAgi();
    if (!StringIsEmpty(localGroupId)) {
      this.roleForm.id = isNaN(Number(localGroupId)) ? localGroupId : Number(localGroupId)
      this.roleForm.name = this.roleGroupList.find((g) => g.gid === this.roleForm.id)?.gname
    }

    this.$eventBus.$on('ToggleAuthGroupStatus', this, (val) => {
      this.groupEnabled = !!val
    })

    // role group
    this.roleOptions = deepClone([...this.roleList])
    this.$eventBus.$on('RoleGroupRefresh', this, () => {
      this.roleGroupRefresh()
    })
  }

  private mounted () {
    document.addEventListener('visibilitychange', this.visibilitychange)
  }

  private beforeDestroy() {
    this.$eventBus.$off('ToggleAuthGroupStatus')
    this.$eventBus.$off('RoleGroupRefresh')
    document.removeEventListener('visibilitychange', this.visibilitychange)
  }

  // 监听浏览器页签切换事件
  private visibilitychange () {
    let localGroupId = getAgi();
    localGroupId = isNaN(Number(localGroupId)) ? localGroupId : Number(localGroupId)
    const groupChanged = this.groupEnabled && this.getCurrGroupId !== localGroupId;
    if (groupChanged && document.hidden === false) {
      window.location.reload()
    }
  }

  private toggleAccountGroupHandle () {
    const localNextShowTip = window.localStorage.getItem('DATABUFF_AGI_NEXT_SHOW')
    if (StringIsEmpty(localNextShowTip) || localNextShowTip === '1') {
      // 初次或者没有勾选不再提示，则继续显示弹窗
      this.showToggleConfirmDialog = true
    } else {
      // 上次选了不再提示后，直接切换管理域
      this.postUgHandle();
    }
  }

  private cancelUgHandle () {
    this.nextToggleShowTip = false;
    this.showToggleConfirmDialog = false
  }

  private postUgHandle () {
    this.changeGroupHandle();
    if (this.nextToggleShowTip) {
      window.localStorage.setItem('DATABUFF_AGI_NEXT_SHOW', '0')
    }
    if (window.axiosCancel.length !== 0) {
      for (const func of window.axiosCancel) {
        setTimeout(func('interrupt'), 0);
      }
      window.axiosCancel = [];
    }
    this.$eventBus.$emit('GlobalRefresh');
    this.cancelUgHandle();
    window.location.reload();
  }

  private handleCheckChange (params: any) {
    const { roleId } = params
    this.choosedRoleId = roleId;
    this.choosedRole = deepClone(params);
  }

  private changeGroupHandle () {
    if (typeof this.roleForm.id === 'string') {
      const groups: any[] = [...this.currRoleGroupRela];
      this.setCurrentGroup(deepClone(groups));
    } else {
      const targetGroup = this.roleGroupList?.find((g) => g.gid === this.roleForm.id);
      this.setCurrentGroup(deepClone([targetGroup]));
    }
  }

  private hideResetRoleIdHandle () {
    this.choosedRoleId = this.getCurrRoleId;
    if (this.$refs.roleTree && this.$refs.roleTree?.setCurrentKey) {
      this.$refs.roleTree?.setCurrentKey(this.choosedRoleId);
    }
  }

  private roleGroupRefresh () {
    const groups: any[] = [...this.currRoleGroupRela].filter(g => g.gid && g.gname) || [];
    const hasConfigAuth = groups.some((g) => g.configAuth);
    if (groups.length > 1) {
      groups.push({
        gid: groups.map((g) => g.gid).join(','),
        gname: groups.map((g) => g.gname).join(' + '),
        dataAuth: true,
        configAuth: hasConfigAuth,
      });
    }
    this.roleGroupList = deepClone(groups);
    this.roleOptions = deepClone(this.roleList)
  }

  private suspendCurrentGroupHandle () {
    if (!this.currGroup.length) {
      return;
    }
    this.setPrevGroup(deepClone(this.currGroup));
    this.setCurrentGroup([]);
  }

  private restorePrevGroupHandle () {
    if (this.currGroup.length || !this.prevGroup.length) {
      return;
    }
    const availableGroups = [...this.currRoleGroupRela].filter(g => g.gid && g.gname);
    if (!availableGroups.length) {
      return;
    }
    const restoredGroups = this.prevGroup
      .map((item: any) => availableGroups.find((group: any) => `${group.gid}` === `${item.gid}`))
      .filter((item: any) => !!item);
    const nextGroups = restoredGroups.length ? restoredGroups : [availableGroups[0]];
    this.setPrevGroup(deepClone(nextGroups));
    this.setCurrentGroup(deepClone(nextGroups));
  }
}
</script>

<style lang="scss" scoped>
.user-group-trigger {
  display: block;
  font-size: 13px;
  background-color: var(--background-color-base);
  color: var(--color-text-primary);
  padding: 0 8px;
  height: 32px;
  line-height: 32px;
  border-radius: 4px;
  cursor: pointer;
  transition: all .3s ease;
  max-width: 300px;

  &:hover {
    color: var(--color-primary);
    background: #e8f2fc;
    border-color: #a2cbf1;
  }
}

.role-group-wrapper {
  width: 400px;
  min-height: 240px;
  align-items: flex-start;
  flex-wrap: wrap;
  overflow: hidden;

  .role-group-tree {
    flex: 1.5;
    max-height: 400px;
    overflow-y: auto;
    border-right: 1px solid var(--border-color-light);
  }
  .role-group-list {
    flex: 1;
    overflow: hidden;
    height: 100%;
    padding-left: 10px;
    max-height: 400px;
    overflow-y: auto;

    .role-group-split {
      margin: 15px 0;
      :deep(.el-divider__text.is-center) {
        padding: 0 10px;
        font-size: 13px;
      }
    }
    
    .role-group-radio-group {
      display: flex;
      flex-direction: column;
    }
    .role-group-radio {
      font-size: 12px;
      font-weight: normal;
      line-height: 20px;
      // display: block;
      margin-right: 0;
      margin-bottom: 4px;
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow: hidden;
      padding-left: 2px;


      :deep(.el-radio__label) {
        font-size: 12px;
        
      }
    }
  }
  .role-group-footer {
    width: 100%;
    text-align: right;
    margin-top: 10px;
  }
  :deep(.el-tree-node__label) {
    font-size: 12px;
  }
  :deep(.el-checkbox) {
    font-size: 12px;
  }
}

.toggle-confirm-cont {
  display: flex;
  align-items: center;
}
.toggle-confirm-icon {
  color: rgb(255, 153, 0);
  font-size: 30px;
}
.toggle-confirm-next-show {
  margin-left: 7px;
}
.toggle-confirm-footer {
  align-items: center;
  margin-top: 30px;
}
</style>
