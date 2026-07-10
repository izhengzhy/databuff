<template>
  <ai-platform-page
    :title="$t('modules.views.aiPlatform.experts.s_95e85417')"
    :subtitle="$t('modules.views.aiPlatform.experts.s_031e25d7')"
    icon="el-icon-user"
    :loading="loading"
  >
    <template slot="stats">
      <stat-pill :label="$t('modules.views.aiPlatform.experts.s_b7e19c8b')" :value="rows.length" accent />
      <stat-pill :label="$t('modules.views.help.startGuide.s_53ace430')" :value="enabledCount" />
      <stat-pill :label="$t('modules.views.aiPlatform.experts.s_a188cfc7')" :value="brainCount" />
    </template>

    <template slot="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openEditor()">{{ $t('modules.views.aiPlatform.experts.s_1f8c8feb') }}</el-button>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          size="small"
          clearable
          prefix-icon="el-icon-search"
          class="filter-input"
          :placeholder="$t('modules.views.aiPlatform.experts.s_87678fdc')"
        />
        <el-radio-group v-model="quickFilter" size="small" class="segmented-filter">
          <el-radio-button label="all">{{ $t('modules.views.aiPlatform.experts.s_a8b0c204') }}</el-radio-button>
          <el-radio-button label="enabled">{{ $t('modules.views.help.startGuide.s_7854b52a') }}</el-radio-button>
          <el-radio-button label="brain">{{ $t('modules.views.aiPlatform.experts.s_5ef3cdc4') }}</el-radio-button>
        </el-radio-group>
      </div>
    </template>

    <div class="resource-board">
      <aside class="resource-rail">
        <div class="rail-title">{{ $t('modules.views.aiPlatform.experts.s_d0d0df58') }}</div>
        <button
          v-for="item in railFilters"
          :key="item.value"
          type="button"
          :class="['rail-item', { 'is-active': categoryFilter === item.value }]"
          @click="categoryFilter = item.value"
        >
          <span>{{ item.labelKey ? $t(item.labelKey) : item.label }}</span>
          <span class="rail-count">{{ item.count }}</span>
        </button>
      </aside>
      <section class="resource-main">
        <div class="table-meta-bar">
          <div>
            <div class="table-title">{{ $t('modules.views.aiPlatform.experts.s_599933c8') }}</div>
            <div class="table-desc">{{ $t('modules.views.aiPlatform.experts.s_8ac8cd6e', { value0: filteredRows.length, value1: rows.length }) }}</div>
          </div>
        </div>
        <div v-if="groupedRows.length" class="category-stack">
          <section v-for="group in groupedRows" :key="group.category" class="category-section">
            <div class="category-head">
              <div>
                <div class="category-title">{{ group.category }}</div>
                <div class="category-desc">{{ $t('modules.views.aiPlatform.experts.s_429a1cfe') }}</div>
              </div>
              <span class="category-count">{{ group.rows.length }}</span>
            </div>
            <el-table :data="group.rows" class="resource-table category-table">
              <el-table-column prop="expertId" label="Expert" min-width="220">
                <template slot-scope="{ row }">
                  <div class="primary-cell">
                    <span class="mono-text">{{ row.expertId }}</span>
                    <div class="primary-name">{{ row.nameKey ? $t(row.nameKey) : row.name }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="type" :label="$t('modules.views.aiPlatform.experts.s_226b0912')" width="130">
                <template slot-scope="{ row }">
                  <span class="source-tag">{{ row.type || 'CUSTOM' }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('modules.views.aiPlatform.experts.s_89acdd31')" min-width="160">
                <template slot-scope="{ row }">
                  <span class="source-tag">{{ (row.toolIds || []).length }} Tools</span>
                  <span class="source-tag ml-6">{{ (row.skillIds || []).length }} Skills</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('modules.views.aiPlatform.experts.s_3fea7ca7')" width="100">
                <template slot-scope="{ row }">
                  <span :class="['status-tag', row.enabled ? 'is-on' : 'is-off']">{{ row.enabled ? $t('modules.views.aiPlatform.experts.s_7854b52a') : $t('modules.views.aiPlatform.experts.s_5c56a889')  }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('modules.views.aiPlatform.experts.s_2b6bc0f2')" width="200" fixed="right">
                <template slot-scope="{ row }">
                  <span class="action-link" @click="openEditor(row)">{{ $t('modules.views.hide.advancedConfig.s_95b351c8') }}</span>
                  <span class="action-link" @click="toggleEnable(row)">{{ row.enabled ? $t('modules.views.aiPlatform.experts.s_5c56a889') : $t('modules.views.aiPlatform.experts.s_7854b52a')  }}</span>
                  <span v-if="!row.builtIn" class="action-link danger" @click="removeExpert(row)">{{ $t('modules.views.hide.advancedConfig.s_2f4aaddd') }}</span>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </div>
        <div v-else class="category-empty">{{ $t('modules.views.aiPlatform.experts.s_0ab9d46f') }}</div>
      </section>
    </div>

    <el-drawer
      :visible.sync="drawerVisible"
      :title="editorTitle"
      size="640px"
      :wrapper-closable="false"
      custom-class="ai-platform-drawer"
    >
      <div class="drawer-content">
        <el-form ref="expertForm" :model="form" :rules="rules" label-width="104px" size="small" class="platform-form">
          <div class="form-section">
            <div class="form-section-head">
              <div class="form-section-title">{{ $t('modules.views.aiPlatform.experts.s_ba11080b') }}</div>
              <div class="form-section-desc">{{ $t('modules.views.aiPlatform.experts.s_1562806e') }}</div>
            </div>
            <div class="form-grid">
              <el-form-item label="Expert ID" prop="expertId">
                <el-input v-model="form.expertId" :disabled="!!editingId" maxlength="64" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.experts.s_d7ec2d3f')" prop="name">
                <el-input v-model="form.name" maxlength="64" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.experts.s_226b0912')">
                <el-select v-model="form.type" class="full-width">
                  <el-option label="BRAIN" value="BRAIN" />
                  <el-option label="SPECIALIST" value="SPECIALIST" />
                  <el-option label="CUSTOM" value="CUSTOM" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.experts.s_d0771a42')">
                <el-input v-model="form.category" maxlength="32" :placeholder="$t('modules.views.aiPlatform.experts.s_60830bcb')" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.experts.s_3fea7ca7')">
                <el-switch v-model="form.enabled" :active-text="$t('modules.views.help.startGuide.s_7854b52a')" :inactive-text="$t('modules.views.aiPlatform.experts.s_5c56a889')" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.configInstall.apm.s_3bdd08ad')" class="is-wide">
                <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="$t('modules.views.aiPlatform.experts.s_f810b393')" />
              </el-form-item>
            </div>
          </div>
          <div class="form-section">
            <div class="form-section-head">
              <div class="form-section-title">{{ $t('modules.views.aiPlatform.experts.s_8e74c14c') }}</div>
              <div class="form-section-desc">{{ $t('modules.views.aiPlatform.experts.s_90da86cb') }}</div>
            </div>
            <el-form-item :label="$t('modules.views.aiPlatform.experts.s_6ac25425')">
              <el-radio-group v-model="form.options.toolAccessMode">
                <el-radio-button label="ALLOWLIST">{{ $t('modules.views.aiPlatform.experts.s_f6f18e09') }}</el-radio-button>
                <el-radio-button label="BLOCKLIST">{{ $t('modules.views.aiPlatform.experts.s_bcbfe2f2') }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="Tools">
              <el-select v-model="form.toolIds" multiple filterable class="full-width">
                <el-option v-for="item in tools" :key="item.toolId" :label="item.toolId" :value="item.toolId" />
              </el-select>
            </el-form-item>
            <el-form-item label="Skills">
              <el-select v-model="form.skillIds" multiple filterable class="full-width">
                <el-option v-for="item in skills" :key="item.skillId" :label="item.skillId" :value="item.skillId" />
              </el-select>
            </el-form-item>
            <el-form-item label="Prompt">
              <el-input v-model="form.systemPrompt" type="textarea" :rows="7" :placeholder="$t('modules.views.aiPlatform.experts.s_7e82489e')" />
            </el-form-item>
            <el-form-item label="Tool Events">
              <el-switch v-model="form.options.exposeToolEvents" :active-text="$t('modules.views.aiPlatform.experts.s_80710e5c')" :inactive-text="$t('modules.views.aiPlatform.experts.s_dce5379c')" />
            </el-form-item>
          </div>
        </el-form>
      </div>
      <div class="drawer-footer">
        <el-button size="small" @click="drawerVisible = false">{{ $t('modules.views.aiPlatform.experts.s_625fb26b') }}</el-button>
        <el-button type="primary" size="small" :loading="saving" @click="saveExpert">{{ $t('modules.views.configInstall.plugin.s_be5fbbe3') }}</el-button>
      </div>
    </el-drawer>
  </ai-platform-page>
</template>

<script lang="ts">
import { Vue, Component } from 'vue-property-decorator';
import i18n from '@/i18n';
import { Form } from 'element-ui';
import AiPlatformPage from '../components/AiPlatformPage.vue';
import StatPill from '../components/stat-pill.vue';
import AiPlatformApi, { AiExpertDefinition, AiSkillDefinition, AiToolDefinition, ExpertRuntimeOptions } from '@/api/aiPlatform';
import { EXPERT_NAME_KEYS } from '../chat/constants';
import { toAsyncWait } from '@/utils/common';

@Component({
  components: { AiPlatformPage, StatPill },
})
export default class AiPlatformExperts extends Vue {
  public $refs!: { expertForm: Form }

  private loading = false
  private saving = false
  private drawerVisible = false
  private editingId = ''
  private keyword = ''
  private quickFilter = 'all'
  private categoryFilter = 'all'
  private rows: AiExpertDefinition[] = []
  private tools: AiToolDefinition[] = []
  private skills: AiSkillDefinition[] = []
  private form = this.emptyForm()

  private rules = {
    expertId: [{ required: true, message: i18n.t('modules.views.aiPlatform.experts.s_7a951975') as string, messageKey: 'modules.views.aiPlatform.experts.s_7a951975', trigger: 'blur' }],
    name: [{ required: true, message: i18n.t('modules.views.aiPlatform.experts.s_06e2f88f') as string, messageKey: 'modules.views.aiPlatform.experts.s_06e2f88f', trigger: 'blur' }],
  }

  get editorTitle () {
    return this.editingId ? i18n.t('modules.views.aiPlatform.experts.s_f05c3dae') as string : i18n.t('modules.views.aiPlatform.experts.s_991362d1') as string
  }

  get enabledCount () {
    return this.rows.filter(item => item.enabled).length
  }

  get brainCount () {
    return this.rows.filter(item => item.type === 'BRAIN').length
  }

  get specialistCount () {
    return this.rows.filter(item => item.type === 'SPECIALIST').length
  }

  get disabledCount () {
    return this.rows.filter(item => !item.enabled).length
  }

  get railFilters () {
    const categories = this.groupRows(this.rows)
    return [{ label: i18n.t('modules.views.aiPlatform.experts.s_1a750305') as string, labelKey: 'modules.views.aiPlatform.experts.s_1a750305', value: 'all', count: this.rows.length }]
      .concat(categories.map(group => ({ label: group.category, value: group.category, count: group.rows.length })))
  }

  get filteredRows () {
    const keyword = this.keyword.trim().toLowerCase()
    return this.rows.filter((item) => {
      const matchesFilter = this.quickFilter === 'all'
        || (this.quickFilter === 'enabled' && item.enabled)
        || (this.quickFilter === 'disabled' && !item.enabled)
        || (this.quickFilter === 'brain' && item.type === 'BRAIN')
        || (this.quickFilter === 'specialist' && item.type === 'SPECIALIST')
      const matchesCategory = this.categoryFilter === 'all' || this.categoryOf(item) === this.categoryFilter
      if (!matchesFilter || !matchesCategory) {
        return false
      }
      if (!keyword) {
        return true
      }
      return [item.expertId, item.name, item.description, item.type, this.categoryOf(item), item.systemPrompt, ...(item.toolIds || []), ...(item.skillIds || [])]
        .some(text => String(text || '').toLowerCase().includes(keyword))
    })
  }

  get groupedRows () {
    return this.groupRows(this.filteredRows)
  }

  private async created () {
    await Promise.all([this.loadOptions(), this.loadData()])
  }

  private emptyForm () {
    return {
      expertId: '',
      name: '',
      category: i18n.t('modules.views.aiPlatform.experts.s_d4296303') as string, categoryKey: 'modules.views.aiPlatform.experts.s_d4296303',
      description: '',
      type: 'CUSTOM',
      systemPrompt: '',
      toolIds: [] as string[],
      skillIds: [] as string[],
      options: this.defaultOptions(),
      enabled: true,
    }
  }

  private defaultOptions (): ExpertRuntimeOptions {
    return {
      stream: false,
      enablePlan: false,
      dynamicSkillsEnabled: false,
      exposeToolEvents: true,
      toolAccessMode: 'ALLOWLIST',
    }
  }

  private async loadOptions () {
    const [toolRes, skillRes] = await Promise.all([
      toAsyncWait(AiPlatformApi.listTools(), false),
      toAsyncWait(AiPlatformApi.listSkills(), false),
    ])
    if (!toolRes.error) {
      this.tools = toolRes.result || []
    }
    if (!skillRes.error) {
      this.skills = skillRes.result || []
    }
  }

  private async loadData () {
    this.loading = true
    const { result, error } = await toAsyncWait(AiPlatformApi.listExperts(), false)
    this.loading = false
    if (!error) {
      this.rows = (result || []).map(row => ({
        ...row,
        nameKey: EXPERT_NAME_KEYS[row.expertId] || (row as any).nameKey,
      }))
    } else if (error.message !== 'interrupt') {
      this.$message.error(error.message)
    }
  }

  private openEditor (row?: AiExpertDefinition) {
    this.editingId = row?.expertId || ''
    this.form = row ? {
      expertId: row.expertId,
      name: row.name,
      category: this.categoryOf(row),
      description: row.description || '',
      type: row.type || 'CUSTOM',
      systemPrompt: row.systemPrompt || '',
      toolIds: [...(row.toolIds || [])],
      skillIds: [...(row.skillIds || [])],
      options: { ...this.defaultOptions(), ...(row.options || {}) },
      enabled: row.enabled,
    } : this.emptyForm()
    this.drawerVisible = true
  }

  private saveExpert () {
    this.$refs.expertForm.validate(async (valid) => {
      if (!valid) {
        return
      }
      const payload = {
        ...this.form,
        category: this.normalizedCategory(this.form.category),
        options: {
          ...this.form.options,
          category: this.normalizedCategory(this.form.category),
        },
      }
      this.saving = true
      const action = this.editingId
        ? AiPlatformApi.updateExpert(this.editingId, payload)
        : AiPlatformApi.createExpert(payload)
      const { error } = await toAsyncWait(action, false)
      this.saving = false
      if (!error) {
        this.$message.success(i18n.t('modules.views.aiPlatform.experts.s_3b108349') as string)
        this.drawerVisible = false
        this.loadData()
      } else if (error.message !== 'interrupt') {
        this.$message.error(error.message)
      }
    })
  }

  private async debugExpert (row: AiExpertDefinition) {
    const { value } = await this.$prompt(i18n.t('modules.views.aiPlatform.experts.s_52447ad8') as string, i18n.t('modules.views.aiPlatform.experts.s_51c9c7d4', { value0: row.name }) as string, {
      inputValue: 'hello',
    }).catch(() => ({ value: '' }))
    if (!value) {
      return
    }
    const { result, error } = await toAsyncWait(AiPlatformApi.debugExpert(row.expertId, value), false)
    if (!error) {
      this.$alert((result as any)?.reply || (result as any)?.error || i18n.t('modules.views.aiPlatform.experts.s_5d6ec8d2') as string, i18n.t('modules.views.aiPlatform.experts.s_21a5ac34') as string)
    } else if (error.message !== 'interrupt') {
      this.$message.error(error.message)
    }
  }

  private categoryOf (row: AiExpertDefinition) {
    return this.normalizedCategory(row.category || row.options?.category || this.guessCategory(row))
  }

  private guessCategory (row: AiExpertDefinition) {
    if (row.type === 'BRAIN') return i18n.t('modules.views.aiPlatform.experts.s_a188cfc7') as string
    if (row.expertId === 'data' || String(row.name || '').includes(i18n.t('modules.views.aiPlatform.experts.s_9bea7657') as string)) return i18n.t('modules.views.aiPlatform.experts.s_6450d843') as string
    if (row.expertId === 'inspection' || String(row.name || '').includes(i18n.t('modules.views.aiPlatform.experts.s_50374573') as string)) return i18n.t('modules.views.aiPlatform.experts.s_21e5c5fa') as string
    if (row.type === 'SPECIALIST') return i18n.t('modules.views.aiPlatform.experts.s_68fbf83f') as string
    return i18n.t('modules.views.aiPlatform.experts.s_d4296303') as string
  }

  private normalizedCategory (value: unknown) {
    return String(value || '').trim() || i18n.t('modules.views.aiPlatform.experts.s_d4296303') as string
  }

  private groupRows (rows: AiExpertDefinition[]) {
    const map = new Map<string, AiExpertDefinition[]>()
    rows.forEach((row) => {
      const category = this.categoryOf(row)
      map.set(category, [...(map.get(category) || []), row])
    })
    return Array.from(map.entries()).map(([category, groupRows]) => ({ category, rows: groupRows }))
  }

  private async reloadExpert (row: AiExpertDefinition) {
    const { error } = await toAsyncWait(AiPlatformApi.reloadExpert(row.expertId), false)
    if (!error) {
      this.$message.success(i18n.t('modules.views.aiPlatform.experts.s_bc81414b') as string)
    } else if (error.message !== 'interrupt') {
      this.$message.error(error.message)
    }
  }

  private async toggleEnable (row: AiExpertDefinition) {
    const action = row.enabled ? AiPlatformApi.disableExpert(row.expertId) : AiPlatformApi.enableExpert(row.expertId)
    const { error } = await toAsyncWait(action, false)
    if (!error) {
      this.loadData()
    } else if (error.message !== 'interrupt') {
      this.$message.error(error.message)
    }
  }

  private removeExpert (row: AiExpertDefinition) {
    this.$confirm(i18n.t('modules.views.aiPlatform.experts.s_72960082', { value0: row.expertId }) as string, i18n.t('common.hint') as string, { type: 'warning' })
      .then(async () => {
        const { error } = await toAsyncWait(AiPlatformApi.deleteExpert(row.expertId), false)
        if (!error) {
          this.$message.success(i18n.t('modules.views.aiPlatform.experts.s_0007d170') as string)
          this.loadData()
        } else if (error.message !== 'interrupt') {
          this.$message.error(error.message)
        }
      })
      .catch(() => undefined)
  }
}
</script>

<style lang="scss" scoped>
.full-width {
  width: 100%;
}

.ml-6 {
  margin-left: 6px;
}
</style>
