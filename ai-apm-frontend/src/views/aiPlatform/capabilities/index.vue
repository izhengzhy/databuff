<template>
  <ai-platform-page
    :title="$t('modules.views.aiPlatform.capabilities.s_f3c11d27')"
    :subtitle="$t('modules.views.aiPlatform.capabilities.s_6825b886')"
    icon="el-icon-magic-stick"
    :loading="loading"
  >
    <template slot="stats">
      <stat-pill :label="$t('modules.views.aiPlatform.capabilities.s_d8b51f69')" :value="rows.length" accent />
    </template>

    <template slot="toolbar">
      <div class="toolbar-right">
        <el-button type="text" size="small" icon="el-icon-refresh" @click="loadData">{{ $t('modules.views.aiPlatform.chat.s_694fc5ef') }}</el-button>
      </div>
    </template>

    <div class="capability-board">
      <div class="table-meta-bar">
        <div>
          <div class="table-title">{{ $t('modules.views.aiPlatform.capabilities.s_f3c11d27') }}</div>
          <div class="table-desc">{{ $t('modules.views.aiPlatform.capabilities.s_6825b886') }}</div>
        </div>
      </div>
      <el-table :data="rows" class="resource-table" v-loading="loading">
        <el-table-column :label="$t('modules.views.aiPlatform.capabilities.s_faaadc44')" width="70" align="center">
          <template slot-scope="{ row, $index }">
            <span :class="['cap-badge', `badge-${row.color}`]">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('modules.views.aiPlatform.experts.s_89acdd31')" min-width="160">
          <template slot-scope="{ row }">
            <span class="primary-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tagline" :label="$t('modules.views.aiPlatform.capabilities.s_039958e1')" min-width="220">
          <template slot-scope="{ row }">
            <span class="tagline-text">{{ row.tagline || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('modules.views.aiPlatform.capabilities.s_62783e84')" width="140">
          <template slot-scope="{ row }">
            <span class="source-tag">{{ row.expertId ? expertDisplayName(row.expertId) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('modules.views.aiPlatform.capabilities.s_89a07c5e')" width="100" align="center">
          <template slot-scope="{ row }">
            <span class="prompt-count">{{ (row.prompts || []).length }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('modules.views.aiPlatform.experts.s_2b6bc0f2')" width="100" fixed="right">
          <template slot-scope="{ row }">
            <span class="action-link" @click="openEditor(row)">{{ $t('modules.views.hide.advancedConfig.s_95b351c8') }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer
      :visible.sync="drawerVisible"
      :title="$t('modules.views.aiPlatform.capabilities.s_862b2e89')"
      size="640px"
      :wrapper-closable="false"
      custom-class="ai-platform-drawer"
    >
      <div class="drawer-content">
        <el-form ref="capForm" :model="form" :rules="rules" label-width="104px" size="small" class="platform-form">
          <div class="form-section">
            <div class="form-section-head">
              <div class="form-section-title">{{ $t('modules.views.aiPlatform.capabilities.s_862b2e89') }}</div>
              <div class="form-section-desc">{{ form.capabilityId }}</div>
            </div>
            <div class="form-grid">
              <el-form-item :label="$t('modules.views.aiPlatform.capabilities.s_394bcf8d')" prop="name">
                <el-input v-model="form.name" maxlength="64" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.capabilities.s_62783e84')" prop="expertId">
                <el-select v-model="form.expertId" filterable clearable :placeholder="$t('modules.views.aiPlatform.capabilities.s_6825b886')" class="full-width">
                  <el-option v-for="item in experts" :key="item.expertId" :label="item.name" :value="item.expertId" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.capabilities.s_039958e1')" class="is-wide">
                <el-input v-model="form.tagline" maxlength="120" />
              </el-form-item>
            </div>
          </div>
          <div class="form-section">
            <div class="form-section-head">
              <div class="form-section-title">{{ $t('modules.views.aiPlatform.capabilities.s_e70cb7f3') }}</div>
              <div class="form-section-desc">{{ $t('modules.views.aiPlatform.capabilities.s_6825b886') }}</div>
            </div>
            <el-form-item
              v-for="(prompt, idx) in form.prompts"
              :key="idx"
              :label="$t('modules.views.aiPlatform.capabilities.s_a808298f', { value0: idx + 1 })"
            >
              <el-input v-model="form.prompts[idx]" type="textarea" :rows="3" />
            </el-form-item>
          </div>
        </el-form>
      </div>
      <div class="drawer-footer">
        <el-button size="small" @click="drawerVisible = false">{{ $t('modules.views.aiPlatform.experts.s_625fb26b') }}</el-button>
        <el-button type="primary" size="small" :loading="saving" @click="saveCapability">{{ $t('modules.views.configInstall.plugin.s_be5fbbe3') }}</el-button>
      </div>
    </el-drawer>
  </ai-platform-page>
</template>

<script lang="ts">
import { Vue, Component } from 'vue-property-decorator'
import i18n from '@/i18n'
import { Form } from 'element-ui'
import AiPlatformPage from '../components/AiPlatformPage.vue'
import StatPill from '../components/stat-pill.vue'
import AiPlatformApi, { AiCapabilityDefinition, AiExpertDefinition } from '@/api/aiPlatform'
import { EXPERT_NAME_KEYS } from '../chat/constants'
import { toAsyncWait } from '@/utils/common'

@Component({
  components: { AiPlatformPage, StatPill },
})
export default class AiPlatformCapabilities extends Vue {
  public $refs!: { capForm: Form }

  private loading = false
  private saving = false
  private drawerVisible = false
  private editingId = ''
  private rows: AiCapabilityDefinition[] = []
  private experts: AiExpertDefinition[] = []
  private form = this.emptyForm()

  private rules = {
    name: [{ required: true, message: i18n.t('modules.views.aiPlatform.experts.s_06e2f88f') as string, messageKey: 'modules.views.aiPlatform.experts.s_06e2f88f', trigger: 'blur' }],
  }

  private expertDisplayName (expertId: string): string {
    const builtInKey = EXPERT_NAME_KEYS[expertId]
    if (builtInKey) {
      return i18n.t(builtInKey) as string
    }
    const expert = this.experts.find(item => item.expertId === expertId)
    return expert?.name || expertId
  }

  private async created () {
    await Promise.all([this.loadExperts(), this.loadData()])
  }

  private emptyForm () {
    return {
      capabilityId: '',
      name: '',
      tagline: '',
      expertId: '',
      prompts: ['', '', '', ''] as string[],
      enabled: true,
    }
  }

  private async loadExperts () {
    const { result, error } = await toAsyncWait(AiPlatformApi.listExperts(), false)
    if (!error) {
      this.experts = result || []
    }
  }

  private async loadData () {
    this.loading = true
    try {
      const { result, error } = await toAsyncWait(AiPlatformApi.listCapabilities(), false)
      if (!error) {
        this.rows = result || []
      }
    } finally {
      this.loading = false
    }
  }

  private openEditor (row: AiCapabilityDefinition) {
    this.editingId = row.capabilityId
    const prompts = (row.prompts && row.prompts.length) ? [...row.prompts] : ['', '', '', '']
    while (prompts.length < 4) {
      prompts.push('')
    }
    this.form = {
      capabilityId: row.capabilityId,
      name: row.name,
      tagline: row.tagline || '',
      expertId: row.expertId,
      prompts,
      enabled: row.enabled,
    }
    this.drawerVisible = true
  }

  private async saveCapability () {
    if (!this.editingId) {
      return
    }
    try {
      await this.$refs.capForm.validate()
    } catch {
      return
    }
    this.saving = true
    try {
      const payload = {
        name: this.form.name.trim(),
        tagline: this.form.tagline.trim(),
        expertId: this.form.expertId,
        prompts: this.form.prompts.map(p => p.trim()).filter(p => p.length > 0),
        enabled: this.form.enabled,
      }
      const { error } = await toAsyncWait(AiPlatformApi.updateCapability(this.editingId, payload), false)
      if (error && error.message !== 'interrupt') {
        this.$message.error(error.message)
        return
      }
      this.$message.success(i18n.t('modules.views.aiPlatform.experts.s_3b108349') as string)
      this.drawerVisible = false
      await this.loadData()
    } finally {
      this.saving = false
    }
  }
}
</script>

<style lang="scss" scoped>
.full-width {
  width: 100%;
}

.capability-board {
  flex: 1;
  min-height: 0;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
}

.cap-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: #fff;
  font-weight: 800;
  font-size: 13px;
}

.badge-opener { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.badge-synth { background: linear-gradient(135deg, #f43f5e, #e11d48); }
.badge-loop { background: linear-gradient(135deg, #f59e0b, #d97706); }
.badge-finale { background: linear-gradient(135deg, #10b981, #059669); }
.badge-closing { background: linear-gradient(135deg, #64748b, #475569); }

.tagline-text {
  color: #5f6f86;
  font-size: 12px;
  line-height: 1.5;
}

.prompt-count {
  font-weight: 700;
  color: #1f5eff;
  font-size: 14px;
}
</style>
