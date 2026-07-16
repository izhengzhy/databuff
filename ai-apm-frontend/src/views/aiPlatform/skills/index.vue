<template>
  <ai-platform-page
    :title="$t('modules.views.aiPlatform.skills.s_735ac607')"
    :subtitle="$t('modules.views.aiPlatform.skills.s_f0dc3f58')"
    icon="el-icon-document"
    :loading="loading"
  >
    <template slot="stats">
      <stat-pill :label="$t('modules.views.aiPlatform.skills.s_56e63773')" :value="rows.length" accent />
      <stat-pill :label="$t('modules.views.help.startGuide.s_53ace430')" :value="enabledCount" />
      <stat-pill :label="$t('modules.views.aiPlatform.skills.s_df0d907b')" :value="builtInCount" />
    </template>

    <template slot="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" size="small" icon="el-icon-upload2" @click="openImportDialog">{{ $t('modules.views.aiPlatform.skills.s_d70cf12d') }}</el-button>
        <el-button size="small" icon="el-icon-plus" @click="openEditor()">{{ $t('modules.views.aiPlatform.skills.s_5ecdb9d5') }}</el-button>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          size="small"
          clearable
          prefix-icon="el-icon-search"
          class="filter-input"
          :placeholder="$t('modules.views.aiPlatform.skills.s_9261429d')"
        />
        <el-radio-group v-model="quickFilter" size="small" class="segmented-filter">
          <el-radio-button label="all">{{ $t('modules.views.aiPlatform.experts.s_a8b0c204') }}</el-radio-button>
          <el-radio-button label="enabled">{{ $t('modules.views.help.startGuide.s_7854b52a') }}</el-radio-button>
          <el-radio-button label="custom">{{ $t('modules.views.aiPlatform.skills.s_f1d4ff50') }}</el-radio-button>
        </el-radio-group>
      </div>
    </template>

    <div class="resource-board">
      <aside class="resource-rail">
        <div class="rail-title">{{ $t('modules.views.aiPlatform.skills.s_9295302c') }}</div>
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
            <div class="table-title">{{ $t('modules.views.aiPlatform.skills.s_176cf124') }}</div>
            <div class="table-desc">{{ $t('modules.views.aiPlatform.skills.s_61eeb1bb', { value0: filteredRows.length, value1: rows.length }) }}</div>
          </div>
        </div>
        <div v-if="groupedRows.length" class="category-stack">
          <section v-for="group in groupedRows" :key="group.categoryKey" class="category-section">
            <div class="category-head">
              <div>
                <div class="category-title">{{ $t(group.categoryKey) }}</div>
                <div class="category-desc">{{ $t('modules.views.aiPlatform.skills.s_690c5556') }}</div>
              </div>
              <span class="category-count">{{ group.rows.length }}</span>
            </div>
            <el-table :data="group.rows" class="resource-table category-table">
              <el-table-column prop="skillId" label="Skill" min-width="220">
                <template slot-scope="{ row }">
                  <div class="primary-cell">
                    <span class="mono-text">{{ row.skillId }}</span>
                    <div class="primary-name">{{ skillNameKey(row) ? $t(skillNameKey(row)) : row.name }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="contentUri" label="Content URI" min-width="320" show-overflow-tooltip>
                <template slot-scope="{ row }">
                  <span class="mono-text uri-cell">{{ row.contentUri }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('modules.views.aiPlatform.experts.s_3fea7ca7')" width="100">
                <template slot-scope="{ row }">
                  <span :class="['status-tag', row.enabled ? 'is-on' : 'is-off']">{{ row.enabled ? $t('modules.views.aiPlatform.experts.s_7854b52a') : $t('modules.views.aiPlatform.experts.s_5c56a889')  }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('modules.views.aiPlatform.skills.s_26ca20b1')" width="90">
                <template slot-scope="{ row }">
                  <span class="source-tag">{{ row.builtIn ? $t('modules.views.aiPlatform.skills.s_89e180d6') : $t('modules.views.aiPlatform.skills.s_f1d4ff50')  }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('modules.views.aiPlatform.experts.s_2b6bc0f2')" width="220" fixed="right">
                <template slot-scope="{ row }">
                  <span class="action-link" @click="openEditor(row)">{{ $t('modules.views.hide.advancedConfig.s_95b351c8') }}</span>
                  <span class="action-link" @click="openFiles(row)">{{ $t('modules.views.aiPlatform.skills.s_2a0c4740') }}</span>
                  <span class="action-link" @click="showReferences(row)">{{ $t('modules.views.aiPlatform.skills.s_3b61c966') }}</span>
                  <span v-if="!row.builtIn" class="action-link danger" @click="removeSkill(row)">{{ $t('modules.views.hide.advancedConfig.s_2f4aaddd') }}</span>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </div>
        <div v-else class="category-empty">{{ $t('modules.views.aiPlatform.skills.s_2a8f4fa2') }}</div>
      </section>
    </div>

    <el-dialog
      :title="$t('modules.views.aiPlatform.skills.s_d70cf12d')"
      :visible.sync="importVisible"
      width="640px"
      :close-on-click-modal="false"
      custom-class="ai-platform-dialog"
      @closed="resetImportForm"
    >
      <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-width="108px" size="small">
        <el-form-item :label="$t('modules.views.aiPlatform.skills.s_d1d4a2cb')">
          <el-upload
            drag
            action=""
            accept=".zip"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            :file-list="importFileList"
            :on-change="handleImportFileChange"
            :on-remove="handleImportFileRemove"
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">{{ $t('modules.views.aiPlatform.skills.s_2ccf044d') }}<em>{{ $t('modules.views.aiPlatform.skills.s_2c808b02') }}</em></div>
            <div slot="tip" class="el-upload__tip">{{ $t('modules.views.aiPlatform.skills.s_a554ab22') }}</div>
          </el-upload>
        </el-form-item>
        <el-form-item :label="$t('modules.views.aiPlatform.skills.s_d40062fc')">
          <el-input v-model="importForm.skillId" disabled :placeholder="$t('modules.views.aiPlatform.skills.s_e01e3a81')" />
        </el-form-item>
        <el-form-item :label="$t('modules.views.aiPlatform.skills.s_879c241d')" prop="name">
          <el-input v-model="importForm.name" maxlength="64" :placeholder="$t('modules.views.aiPlatform.skills.s_e56fc6f9')" />
        </el-form-item>
        <el-form-item :label="$t('modules.views.aiPlatform.experts.s_d0771a42')">
          <el-input v-model="importForm.category" maxlength="32" :placeholder="$t('modules.views.aiPlatform.skills.s_c8f91bb4')" />
        </el-form-item>
        <el-form-item :label="$t('modules.views.configInstall.apm.s_3bdd08ad')">
          <el-input v-model="importForm.description" type="textarea" :rows="3" :placeholder="$t('modules.views.aiPlatform.skills.s_62b1e7f0')" />
        </el-form-item>
        <el-form-item :label="$t('modules.views.aiPlatform.experts.s_3fea7ca7')">
          <el-switch v-model="importForm.enabled" :active-text="$t('modules.views.help.startGuide.s_7854b52a')" :inactive-text="$t('modules.views.aiPlatform.experts.s_5c56a889')" />
        </el-form-item>
        <el-form-item v-if="importPreviewFiles.length" :label="$t('modules.views.aiPlatform.skills.s_1a43baf4')">
          <div class="import-file-list">
            <div v-for="item in importPreviewFiles" :key="item.path" class="import-file-item">
              <span class="mono-text">{{ item.path }}</span>
              <span class="import-file-size">{{ formatSize(item.size) }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button size="small" @click="importVisible = false">{{ $t('modules.views.aiPlatform.experts.s_625fb26b') }}</el-button>
        <el-button type="primary" size="small" :loading="importing" @click="submitImport">{{ $t('modules.views.aiPlatform.skills.s_8d9a071e') }}</el-button>
      </span>
    </el-dialog>

    <el-drawer
      :visible.sync="drawerVisible"
      :title="editorTitle"
      size="520px"
      :wrapper-closable="false"
      custom-class="ai-platform-drawer"
    >
      <div class="drawer-content">
        <el-form ref="skillForm" :model="form" :rules="rules" label-width="96px" size="small" class="platform-form">
          <div class="form-section">
            <div class="form-section-head">
              <div class="form-section-title">{{ $t('modules.views.aiPlatform.skills.s_6ea1fe6b') }}</div>
              <div class="form-section-desc">{{ $t('modules.views.aiPlatform.skills.s_f5a16910') }}</div>
            </div>
            <div class="form-grid">
              <el-form-item label="Skill ID" prop="skillId">
                <el-input v-model="form.skillId" :disabled="!!editingId" maxlength="64" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.skills.s_879c241d')" prop="name">
                <el-input v-model="form.name" maxlength="64" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.experts.s_d0771a42')">
                <el-input v-model="form.category" maxlength="32" :placeholder="$t('modules.views.aiPlatform.skills.s_c8f91bb4')" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.configInstall.apm.s_3bdd08ad')" class="is-wide">
                <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="$t('modules.views.aiPlatform.skills.s_d369e4c7')" />
              </el-form-item>
              <el-form-item :label="$t('modules.views.aiPlatform.experts.s_3fea7ca7')">
                <el-switch v-model="form.enabled" :active-text="$t('modules.views.help.startGuide.s_7854b52a')" :inactive-text="$t('modules.views.aiPlatform.experts.s_5c56a889')" />
              </el-form-item>
            </div>
          </div>
          <div class="form-section">
            <div class="form-section-head">
              <div class="form-section-title">{{ $t('modules.views.aiPlatform.skills.s_e80bb7de') }}</div>
              <div class="form-section-desc">{{ $t('modules.views.aiPlatform.skills.s_b94f50af') }}</div>
            </div>
            <el-form-item label="Content URI" prop="contentUri">
              <el-input v-model="form.contentUri" placeholder="classpath:/ai/skills/.../SKILL.md" />
            </el-form-item>
            <el-form-item label="File Path">
              <el-input v-model="form.filePath" placeholder="./data/skills/{skillId}/SKILL.md" />
            </el-form-item>
            <el-form-item v-if="referenceExperts.length" :label="$t('modules.views.aiPlatform.skills.s_bd801c8f')">
              <el-tag v-for="id in referenceExperts" :key="id" size="mini" class="mr-6">{{ id }}</el-tag>
            </el-form-item>
          </div>
        </el-form>
      </div>
      <div class="drawer-footer">
        <el-button size="small" @click="drawerVisible = false">{{ $t('modules.views.aiPlatform.experts.s_625fb26b') }}</el-button>
        <el-button type="primary" size="small" :loading="saving" @click="saveSkill">{{ $t('modules.views.configInstall.plugin.s_be5fbbe3') }}</el-button>
      </div>
    </el-drawer>

    <el-drawer
      :visible.sync="filesVisible"
      :title="filesTitle"
      size="760px"
      :wrapper-closable="false"
      custom-class="ai-platform-drawer skill-files-drawer"
    >
      <div v-loading="filesLoading" class="skill-files-layout">
        <aside class="skill-files-sidebar">
          <div class="skill-files-sidebar-title">{{ $t('modules.views.aiPlatform.skills.s_1a43baf4') }}</div>
          <button
            v-for="item in skillFiles"
            :key="item.path"
            type="button"
            :class="['skill-file-item', { 'is-active': activeFilePath === item.path }]"
            @click="selectFile(item.path)"
          >
            <span class="mono-text">{{ item.path }}</span>
          </button>
          <div v-if="!filesLoading && !skillFiles.length" class="skill-files-empty">{{ $t('modules.views.aiPlatform.skills.s_725ca79f') }}</div>
        </aside>
        <section class="skill-files-content">
          <div v-if="activeFilePath" class="skill-files-content-head">
            <span class="mono-text">{{ activeFilePath }}</span>
          </div>
          <marked-view
            v-if="activeFileContent && isMarkdownFile(activeFilePath)"
            :data="activeFileContent"
            :show-copy="true"
            class="skill-file-preview"
          />
          <code-view
            v-else-if="activeFileContent"
            :code="activeFileContent"
            :lang="fileLang(activeFilePath)"
            class="skill-file-preview"
          />
          <div v-else class="skill-files-empty">{{ $t('modules.views.aiPlatform.skills.s_6c9f78fa') }}</div>
        </section>
      </div>
    </el-drawer>
  </ai-platform-page>
</template>

<script lang="ts">
import { Vue, Component } from 'vue-property-decorator';
import i18n from '@/i18n';
import { Form } from 'element-ui';
import MarkedView from '@/components/marked-view.vue';
import CodeView from '@/components/code-view.vue';
import AiPlatformPage from '../components/AiPlatformPage.vue';
import StatPill from '../components/stat-pill.vue';
import AiPlatformApi, {
  AiSkillDefinition,
  AiSkillFileEntry,
  AiSkillImportPreview,
} from '@/api/aiPlatform';
import { SKILL_CATEGORY_KEYS, SKILL_NAME_KEYS } from './constants';
import { toAsyncWait } from '@/utils/common';

interface ImportFormState {
  skillId: string;
  name: string;
  category: string;
  description: string;
  enabled: boolean;
}

@Component({
  components: { MarkedView, CodeView, AiPlatformPage, StatPill },
})
export default class AiPlatformSkills extends Vue {
  public $refs!: {
    skillForm: Form;
    importFormRef: Form;
  }

  private loading = false
  private saving = false
  private importing = false
  private filesLoading = false
  private drawerVisible = false
  private importVisible = false
  private filesVisible = false
  private editingId = ''
  private filesSkillId = ''
  private filesTitle = ''
  private referenceExperts: string[] = []
  private keyword = ''
  private quickFilter = 'all'
  private categoryFilter = 'all'
  private rows: AiSkillDefinition[] = []
  private form: any = this.emptyForm()
  private importForm: ImportFormState = this.emptyImportForm()
  private importFile: File | null = null
  private importFileList: Array<{ name: string }> = []
  private importPreviewFiles: AiSkillFileEntry[] = []
  private skillFiles: AiSkillFileEntry[] = []
  private activeFilePath = ''
  private activeFileContent = ''

  private rules = {
    skillId: [{ required: true, message: i18n.t('modules.views.aiPlatform.skills.s_14fb013e') as string, messageKey: 'modules.views.aiPlatform.skills.s_14fb013e', trigger: 'blur' }],
    name: [{ required: true, message: i18n.t('modules.views.aiPlatform.skills.s_0ff7547d') as string, messageKey: 'modules.views.aiPlatform.skills.s_0ff7547d', trigger: 'blur' }],
    contentUri: [{ required: true, message: i18n.t('modules.views.aiPlatform.skills.s_a9600960') as string, messageKey: 'modules.views.aiPlatform.skills.s_a9600960', trigger: 'blur' }],
  }

  private importRules = {
    name: [{ required: true, message: i18n.t('modules.views.aiPlatform.skills.s_0ff7547d') as string, messageKey: 'modules.views.aiPlatform.skills.s_0ff7547d', trigger: 'blur' }],
  }

  get editorTitle () {
    return this.editingId ? i18n.t('modules.views.aiPlatform.skills.s_48b3c4ae') as string : i18n.t('modules.views.aiPlatform.skills.s_05aa2414') as string
  }

  get enabledCount () {
    return this.rows.filter(item => item.enabled).length
  }

  get builtInCount () {
    return this.rows.filter(item => item.builtIn).length
  }

  get customCount () {
    return this.rows.filter(item => !item.builtIn).length
  }

  get disabledCount () {
    return this.rows.filter(item => !item.enabled).length
  }

  get railFilters () {
    const categories = this.groupRows(this.rows)
    return [{ labelKey: 'modules.views.aiPlatform.experts.s_1a750305', value: 'all', count: this.rows.length }]
      .concat(categories.map(group => ({ labelKey: group.categoryKey, value: group.categoryKey, count: group.rows.length })))
  }

  get filteredRows () {
    const keyword = this.keyword.trim().toLowerCase()
    return this.rows.filter((item) => {
      const matchesFilter = this.quickFilter === 'all'
        || (this.quickFilter === 'enabled' && item.enabled)
        || (this.quickFilter === 'disabled' && !item.enabled)
        || (this.quickFilter === 'builtIn' && item.builtIn)
        || (this.quickFilter === 'custom' && !item.builtIn)
      const matchesCategory = this.categoryFilter === 'all' || this.categoryKeyOf(item) === this.categoryFilter
      if (!matchesFilter || !matchesCategory) {
        return false
      }
      if (!keyword) {
        return true
      }
      return [item.skillId, item.name, item.description, item.contentUri, item.filePath, this.categoryOf(item)]
        .some(text => String(text || '').toLowerCase().includes(keyword))
    })
  }

  get groupedRows () {
    return this.groupRows(this.filteredRows)
  }

  private created () {
    this.loadData()
  }

  private emptyForm () {
    return {
      skillId: '',
      name: '',
      category: i18n.t('modules.views.aiPlatform.experts.s_d4296303') as string, categoryKey: 'modules.views.aiPlatform.experts.s_d4296303',
      description: '',
      contentUri: '',
      filePath: '',
      enabled: true,
    }
  }

  private emptyImportForm (): ImportFormState {
    return {
      skillId: '',
      name: '',
      category: i18n.t('modules.views.aiPlatform.experts.s_d4296303') as string, categoryKey: 'modules.views.aiPlatform.experts.s_d4296303',
      description: '',
      enabled: true,
    }
  }

  private formatSize (size: number) {
    if (!size) {
      return '0 B'
    }
    if (size < 1024) {
      return `${size} B`
    }
    if (size < 1024 * 1024) {
      return `${(size / 1024).toFixed(1)} KB`
    }
    return `${(size / (1024 * 1024)).toFixed(1)} MB`
  }

  private isMarkdownFile (path: string) {
    const lower = String(path || '').toLowerCase()
    return lower.endsWith('.md') || lower.endsWith('.markdown')
  }

  private fileLang (path: string) {
    const lower = String(path || '').toLowerCase()
    if (lower.endsWith('.json')) return 'json'
    if (lower.endsWith('.py')) return 'python'
    if (lower.endsWith('.sh')) return 'shell'
    if (lower.endsWith('.yaml') || lower.endsWith('.yml')) return 'yaml'
    return ''
  }

  private async loadData () {
    this.loading = true
    const { result, error } = await toAsyncWait(AiPlatformApi.listSkills(), false)
    this.loading = false
    if (!error) {
      this.rows = result || []
    } else if (error.message !== 'interrupt') {
      this.$message.error(error.message)
    }
  }

  private openImportDialog () {
    this.resetImportForm()
    this.importVisible = true
  }

  private resetImportForm () {
    this.importForm = this.emptyImportForm()
    this.importFile = null
    this.importFileList = []
    this.importPreviewFiles = []
    this.importing = false
  }

  private handleImportFileRemove () {
    this.importFile = null
    this.importFileList = []
    this.importPreviewFiles = []
    this.importForm = this.emptyImportForm()
  }

  private async handleImportFileChange (file: { raw?: File }) {
    const raw = file?.raw
    if (!raw) {
      return
    }
    this.importFile = raw
    this.importFileList = [{ name: raw.name }]
    this.importing = true
    const { result, error } = await toAsyncWait(AiPlatformApi.previewSkillImport(raw), false)
    this.importing = false
    if (error) {
      this.handleImportFileRemove()
      if (error.message !== 'interrupt') {
        this.$message.error(error.message)
      }
      return
    }
    const preview = result as AiSkillImportPreview
    this.importForm.skillId = preview.skillId || preview.skillName || ''
    this.importForm.name = preview.skillName || preview.skillId || ''
    this.importForm.category = this.guessCategory(this.importForm.skillId, this.importForm.name)
    this.importForm.description = preview.description || ''
    this.importPreviewFiles = preview.files || []
  }

  private submitImport () {
    if (!this.importFile) {
      this.$message.warning(i18n.t('modules.views.aiPlatform.skills.s_6e432978') as string)
      return
    }
    if (!this.importForm.skillId) {
      this.$message.warning(i18n.t('modules.views.aiPlatform.skills.s_4b219d15') as string)
      return
    }
    this.$refs.importFormRef.validate(async (valid) => {
      if (!valid || !this.importFile) {
        return
      }
      this.importing = true
      const { error } = await toAsyncWait(AiPlatformApi.importSkill(this.importFile, {
        name: this.importForm.name,
        category: this.normalizedCategory(this.importForm.category),
        description: this.importForm.description,
        enabled: this.importForm.enabled,
      }), false)
      this.importing = false
      if (!error) {
        this.$message.success(i18n.t('modules.views.aiPlatform.skills.s_b6d16a81') as string)
        this.importVisible = false
        this.loadData()
      } else if (error.message !== 'interrupt') {
        this.$message.error(error.message)
      }
    })
  }

  private openEditor (row?: AiSkillDefinition) {
    this.editingId = row?.skillId || ''
    this.referenceExperts = []
    this.form = row ? {
      skillId: row.skillId,
      name: row.name,
      category: this.categoryOf(row),
      description: row.description || '',
      contentUri: row.contentUri,
      filePath: row.filePath || '',
      enabled: row.enabled,
    } : this.emptyForm()
    this.drawerVisible = true
    if (row?.skillId) {
      this.loadReferences(row.skillId)
    }
  }

  private async openFiles (row: AiSkillDefinition) {
    this.filesSkillId = row.skillId
    this.filesTitle = i18n.t('modules.views.aiPlatform.skills.s_502cafff', { value0: row.name }) as string
    this.filesVisible = true
    this.skillFiles = []
    this.activeFilePath = ''
    this.activeFileContent = ''
    this.filesLoading = true
    const { result, error } = await toAsyncWait(AiPlatformApi.skillFiles(row.skillId), false)
    this.filesLoading = false
    if (error) {
      if (error.message !== 'interrupt') {
        this.$message.error(error.message)
      }
      return
    }
    this.skillFiles = result || []
    if (this.skillFiles.length) {
      await this.selectFile(this.skillFiles[0].path)
    }
  }

  private async selectFile (path: string) {
    if (!this.filesSkillId || !path) {
      return
    }
    this.activeFilePath = path
    this.activeFileContent = ''
    const { result, error } = await toAsyncWait(
      AiPlatformApi.skillFileContent(this.filesSkillId, path),
      false,
    )
    if (!error) {
      this.activeFileContent = (result as any)?.content || ''
    } else if (error.message !== 'interrupt') {
      this.$message.error(error.message)
    }
  }

  private async loadReferences (skillId: string) {
    const { result, error } = await toAsyncWait(AiPlatformApi.skillReferences(skillId), false)
    if (!error) {
      this.referenceExperts = (result as any)?.expertIds || []
    }
  }

  private async showReferences (row: AiSkillDefinition) {
    await this.loadReferences(row.skillId)
    const text = this.referenceExperts.length ? this.referenceExperts.join(', ') : i18n.t('modules.views.aiPlatform.skills.s_281fa986') as string
    this.$alert(text, i18n.t('modules.views.aiPlatform.skills.s_b853a08a', { value0: row.skillId }) as string)
  }

  private saveSkill () {
    this.$refs.skillForm.validate(async (valid) => {
      if (!valid) {
        return
      }
      const payload = {
        ...this.form,
        category: this.normalizedCategory(this.form.category),
      }
      this.saving = true
      const action = this.editingId
        ? AiPlatformApi.updateSkill(this.editingId, payload)
        : AiPlatformApi.createSkill(payload)
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

  private async validateSkill (row: AiSkillDefinition) {
    const { error } = await toAsyncWait(AiPlatformApi.validateSkill(row.skillId), false)
    if (!error) {
      this.$message.success(i18n.t('modules.views.aiPlatform.skills.s_9a9088c0') as string)
    } else if (error.message !== 'interrupt') {
      this.$message.error(error.message)
    }
  }

  private skillNameKey (row: AiSkillDefinition) {
    return SKILL_NAME_KEYS[row.skillId]
  }

  private categoryKeyOf (row: AiSkillDefinition) {
    const raw = String(row.category || '').trim()
    if (raw && SKILL_CATEGORY_KEYS[raw]) {
      return SKILL_CATEGORY_KEYS[raw]
    }
    return this.guessCategoryKey(row.skillId, row.name)
  }

  private categoryOf (row: AiSkillDefinition) {
    return i18n.t(this.categoryKeyOf(row)) as string
  }

  private guessCategoryKey (id: string, name = '') {
    const text = `${id || ''} ${name || ''}`.toLowerCase()
    if (text.includes('brain') || text.includes('大脑') || text.includes('routing')) return 'modules.views.aiPlatform.skills.s_2106237c'
    if (text.includes('summary') || text.includes('总结')) return 'modules.views.aiPlatform.skills.s_b0fdef01'
    if (text.includes('data') || text.includes('metric') || text.includes('问数')) return 'modules.views.aiPlatform.experts.s_6450d843'
    if (text.includes('inspection') || text.includes('health') || text.includes('巡检')) return 'modules.views.aiPlatform.experts.s_21e5c5fa'
    return 'modules.views.aiPlatform.experts.s_d4296303'
  }

  private guessCategory (id: string, name = '') {
    return i18n.t(this.guessCategoryKey(id, name)) as string
  }

  private normalizedCategory (value: unknown) {
    return String(value || '').trim() || i18n.t('modules.views.aiPlatform.experts.s_d4296303') as string
  }

  private groupRows (rows: AiSkillDefinition[]) {
    const map = new Map<string, AiSkillDefinition[]>()
    rows.forEach((row) => {
      const categoryKey = this.categoryKeyOf(row)
      map.set(categoryKey, [...(map.get(categoryKey) || []), row])
    })
    return Array.from(map.entries()).map(([categoryKey, groupRows]) => ({ categoryKey, rows: groupRows }))
  }

  private async toggleEnable (row: AiSkillDefinition) {
    const action = row.enabled ? AiPlatformApi.disableSkill(row.skillId) : AiPlatformApi.enableSkill(row.skillId)
    const { error } = await toAsyncWait(action, false)
    if (!error) {
      this.loadData()
    } else if (error.message !== 'interrupt') {
      this.$message.error(error.message)
    }
  }

  private removeSkill (row: AiSkillDefinition) {
    this.$confirm(i18n.t('modules.views.aiPlatform.skills.s_ab6d9907', { value0: row.skillId }) as string, i18n.t('common.hint') as string, { type: 'warning' })
      .then(async () => {
        const { error } = await toAsyncWait(AiPlatformApi.deleteSkill(row.skillId), false)
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
.mr-6 {
  margin-right: 6px;
}

.import-file-list {
  max-height: 180px;
  overflow: auto;
  border: 1px solid #eef1f7;
  border-radius: 8px;
  padding: 8px 12px;
  background: #fafbfd;
}

.import-file-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 0;
  font-size: 12px;
}

.import-file-size {
  color: #909399;
  flex-shrink: 0;
}

.skill-files-layout {
  display: flex;
  height: calc(100vh - 120px);
  min-height: 420px;
  gap: 12px;
}

.skill-files-sidebar {
  width: 240px;
  flex-shrink: 0;
  border: 1px solid #eef1f7;
  border-radius: 8px;
  background: #fafbfd;
  overflow: auto;
  padding: 8px;
}

.skill-files-sidebar-title {
  font-size: 12px;
  color: #909399;
  padding: 4px 8px 8px;
}

.skill-file-item {
  display: block;
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  border-radius: 6px;
  padding: 8px;
  cursor: pointer;
  font-size: 12px;
  color: #303133;

  &.is-active,
  &:hover {
    background: #eef4ff;
    color: #2962ff;
  }
}

.skill-files-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.skill-files-content-head {
  margin-bottom: 8px;
  font-size: 12px;
  color: #606266;
}

.skill-file-preview {
  flex: 1;
  overflow: auto;
  border: 1px solid #eef1f7;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.skill-files-empty {
  color: #909399;
  font-size: 13px;
  padding: 16px;
}
</style>
