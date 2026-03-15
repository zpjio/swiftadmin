<template>
  <div class="cron-editor">
    <NSpace vertical>
      <!-- 快捷选择 -->
      <div class="quick-select">
        <span class="label">快捷选择：</span>
        <NSpace>
          <NButton size="small" @click="setPreset('everyMinute')">每分钟</NButton>
          <NButton size="small" @click="setPreset('everyFiveMinutes')">每5分钟</NButton>
          <NButton size="small" @click="setPreset('everyHour')">每小时</NButton>
          <NButton size="small" @click="setPreset('everyDay')">每天</NButton>
          <NButton size="small" @click="setPreset('everyWeek')">每周</NButton>
          <NButton size="small" @click="setPreset('everyMonth')">每月</NButton>
        </NSpace>
      </div>

      <!-- 表达式说明 -->
      <NAlert type="info" :show-icon="false">
        <div class="expression-info">
          <div class="expression">{{ cronExpression }}</div>
          <div class="description">说明：{{ expressionDescription }}</div>
        </div>
      </NAlert>

      <!-- 可视化编辑器 -->
      <NTabs type="line" animated>
        <NTabPane name="second" tab="秒">
          <NRadioGroup v-model:value="config.second.type" @update:value="updateExpression">
            <div class="radio-group">
              <NRadio value="every">
                <span>每秒</span>
              </NRadio>
              <NRadio value="cycle">
                <span>周期</span>
                <span class="inline-input">
                  <NInputNumber
                    v-model:value="config.second.start"
                    :min="0"
                    :max="59"
                    size="small"
                    style="width: 80px"
                    :disabled="config.second.type !== 'cycle'"
                  />
                  -
                  <NInputNumber
                    v-model:value="config.second.end"
                    :min="0"
                    :max="59"
                    size="small"
                    style="width: 80px"
                    :disabled="config.second.type !== 'cycle'"
                  />
                </span>
              </NRadio>
              <NRadio value="interval">
                <span>从</span>
                <NInputNumber
                  v-model:value="config.second.intervalStart"
                  :min="0"
                  :max="59"
                  size="small"
                  style="width: 80px"
                  :disabled="config.second.type !== 'interval'"
                />
                <span>秒开始，每</span>
                <NInputNumber
                  v-model:value="config.second.intervalValue"
                  :min="1"
                  :max="59"
                  size="small"
                  style="width: 80px"
                  :disabled="config.second.type !== 'interval'"
                />
                <span>秒执行一次</span>
              </NRadio>
              <NRadio value="specific">
                <span>指定</span>
                <NCheckboxGroup v-model:value="config.second.values" :disabled="config.second.type !== 'specific'">
                  <div class="checkbox-grid">
                    <NCheckbox v-for="i in 60" :key="i - 1" :value="i - 1">
                      {{ i - 1 }}
                    </NCheckbox>
                  </div>
                </NCheckboxGroup>
              </NRadio>
            </div>
          </NRadioGroup>
        </NTabPane>

        <NTabPane name="minute" tab="分">
          <NRadioGroup v-model:value="config.minute.type" @update:value="updateExpression">
            <div class="radio-group">
              <NRadio value="every">
                <span>每分</span>
              </NRadio>
              <NRadio value="cycle">
                <span>周期</span>
                <span class="inline-input">
                  <NInputNumber
                    v-model:value="config.minute.start"
                    :min="0"
                    :max="59"
                    size="small"
                    style="width: 80px"
                    :disabled="config.minute.type !== 'cycle'"
                  />
                  -
                  <NInputNumber
                    v-model:value="config.minute.end"
                    :min="0"
                    :max="59"
                    size="small"
                    style="width: 80px"
                    :disabled="config.minute.type !== 'cycle'"
                  />
                </span>
              </NRadio>
              <NRadio value="interval">
                <span>从</span>
                <NInputNumber
                  v-model:value="config.minute.intervalStart"
                  :min="0"
                  :max="59"
                  size="small"
                  style="width: 80px"
                  :disabled="config.minute.type !== 'interval'"
                />
                <span>分开始，每</span>
                <NInputNumber
                  v-model:value="config.minute.intervalValue"
                  :min="1"
                  :max="59"
                  size="small"
                  style="width: 80px"
                  :disabled="config.minute.type !== 'interval'"
                />
                <span>分执行一次</span>
              </NRadio>
              <NRadio value="specific">
                <span>指定</span>
                <NCheckboxGroup v-model:value="config.minute.values" :disabled="config.minute.type !== 'specific'">
                  <div class="checkbox-grid">
                    <NCheckbox v-for="i in 60" :key="i - 1" :value="i - 1">
                      {{ i - 1 }}
                    </NCheckbox>
                  </div>
                </NCheckboxGroup>
              </NRadio>
            </div>
          </NRadioGroup>
        </NTabPane>

        <NTabPane name="hour" tab="时">
          <NRadioGroup v-model:value="config.hour.type" @update:value="updateExpression">
            <div class="radio-group">
              <NRadio value="every">
                <span>每时</span>
              </NRadio>
              <NRadio value="cycle">
                <span>周期</span>
                <span class="inline-input">
                  <NInputNumber
                    v-model:value="config.hour.start"
                    :min="0"
                    :max="23"
                    size="small"
                    style="width: 80px"
                    :disabled="config.hour.type !== 'cycle'"
                  />
                  -
                  <NInputNumber
                    v-model:value="config.hour.end"
                    :min="0"
                    :max="23"
                    size="small"
                    style="width: 80px"
                    :disabled="config.hour.type !== 'cycle'"
                  />
                </span>
              </NRadio>
              <NRadio value="interval">
                <span>从</span>
                <NInputNumber
                  v-model:value="config.hour.intervalStart"
                  :min="0"
                  :max="23"
                  size="small"
                  style="width: 80px"
                  :disabled="config.hour.type !== 'interval'"
                />
                <span>时开始，每</span>
                <NInputNumber
                  v-model:value="config.hour.intervalValue"
                  :min="1"
                  :max="23"
                  size="small"
                  style="width: 80px"
                  :disabled="config.hour.type !== 'interval'"
                />
                <span>时执行一次</span>
              </NRadio>
              <NRadio value="specific">
                <span>指定</span>
                <NCheckboxGroup v-model:value="config.hour.values" :disabled="config.hour.type !== 'specific'">
                  <div class="checkbox-grid">
                    <NCheckbox v-for="i in 24" :key="i - 1" :value="i - 1">
                      {{ i - 1 }}
                    </NCheckbox>
                  </div>
                </NCheckboxGroup>
              </NRadio>
            </div>
          </NRadioGroup>
        </NTabPane>

        <NTabPane name="day" tab="日">
          <NRadioGroup v-model:value="config.day.type" @update:value="updateExpression">
            <div class="radio-group">
              <NRadio value="every">
                <span>每天</span>
              </NRadio>
              <NRadio value="cycle">
                <span>周期</span>
                <span class="inline-input">
                  <NInputNumber
                    v-model:value="config.day.start"
                    :min="1"
                    :max="31"
                    size="small"
                    style="width: 80px"
                    :disabled="config.day.type !== 'cycle'"
                  />
                  -
                  <NInputNumber
                    v-model:value="config.day.end"
                    :min="1"
                    :max="31"
                    size="small"
                    style="width: 80px"
                    :disabled="config.day.type !== 'cycle'"
                  />
                </span>
              </NRadio>
              <NRadio value="interval">
                <span>从</span>
                <NInputNumber
                  v-model:value="config.day.intervalStart"
                  :min="1"
                  :max="31"
                  size="small"
                  style="width: 80px"
                  :disabled="config.day.type !== 'interval'"
                />
                <span>日开始，每</span>
                <NInputNumber
                  v-model:value="config.day.intervalValue"
                  :min="1"
                  :max="31"
                  size="small"
                  style="width: 80px"
                  :disabled="config.day.type !== 'interval'"
                />
                <span>日执行一次</span>
              </NRadio>
              <NRadio value="specific">
                <span>指定</span>
                <NCheckboxGroup v-model:value="config.day.values" :disabled="config.day.type !== 'specific'">
                  <div class="checkbox-grid">
                    <NCheckbox v-for="i in 31" :key="i" :value="i">
                      {{ i }}
                    </NCheckbox>
                  </div>
                </NCheckboxGroup>
              </NRadio>
              <NRadio value="weekday">
                <span>工作日</span>
              </NRadio>
              <NRadio value="last">
                <span>月末</span>
              </NRadio>
            </div>
          </NRadioGroup>
        </NTabPane>

        <NTabPane name="month" tab="月">
          <NRadioGroup v-model:value="config.month.type" @update:value="updateExpression">
            <div class="radio-group">
              <NRadio value="every">
                <span>每月</span>
              </NRadio>
              <NRadio value="cycle">
                <span>周期</span>
                <span class="inline-input">
                  <NInputNumber
                    v-model:value="config.month.start"
                    :min="1"
                    :max="12"
                    size="small"
                    style="width: 80px"
                    :disabled="config.month.type !== 'cycle'"
                  />
                  -
                  <NInputNumber
                    v-model:value="config.month.end"
                    :min="1"
                    :max="12"
                    size="small"
                    style="width: 80px"
                    :disabled="config.month.type !== 'cycle'"
                  />
                </span>
              </NRadio>
              <NRadio value="interval">
                <span>从</span>
                <NInputNumber
                  v-model:value="config.month.intervalStart"
                  :min="1"
                  :max="12"
                  size="small"
                  style="width: 80px"
                  :disabled="config.month.type !== 'interval'"
                />
                <span>月开始，每</span>
                <NInputNumber
                  v-model:value="config.month.intervalValue"
                  :min="1"
                  :max="12"
                  size="small"
                  style="width: 80px"
                  :disabled="config.month.type !== 'interval'"
                />
                <span>月执行一次</span>
              </NRadio>
              <NRadio value="specific">
                <span>指定</span>
                <NCheckboxGroup v-model:value="config.month.values" :disabled="config.month.type !== 'specific'">
                  <div class="checkbox-grid">
                    <NCheckbox v-for="i in 12" :key="i" :value="i">
                      {{ i }}
                    </NCheckbox>
                  </div>
                </NCheckboxGroup>
              </NRadio>
            </div>
          </NRadioGroup>
        </NTabPane>

        <NTabPane name="week" tab="周">
          <NRadioGroup v-model:value="config.week.type" @update:value="updateExpression">
            <div class="radio-group">
              <NRadio value="every">
                <span>每周</span>
              </NRadio>
              <NRadio value="specific">
                <span>指定</span>
                <NCheckboxGroup v-model:value="config.week.values" :disabled="config.week.type !== 'specific'">
                  <div class="checkbox-grid">
                    <NCheckbox :value="1">周日</NCheckbox>
                    <NCheckbox :value="2">周一</NCheckbox>
                    <NCheckbox :value="3">周二</NCheckbox>
                    <NCheckbox :value="4">周三</NCheckbox>
                    <NCheckbox :value="5">周四</NCheckbox>
                    <NCheckbox :value="6">周五</NCheckbox>
                    <NCheckbox :value="7">周六</NCheckbox>
                  </div>
                </NCheckboxGroup>
              </NRadio>
              <NRadio value="last">
                <span>本月最后一个</span>
                <NSelect
                  v-model:value="config.week.lastValue"
                  :options="weekOptions"
                  size="small"
                  style="width: 100px"
                  :disabled="config.week.type !== 'last'"
                />
              </NRadio>
            </div>
          </NRadioGroup>
        </NTabPane>
      </NTabs>
    </NSpace>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'

interface Props {
  modelValue: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const cronExpression = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const expressionDescription = ref('每秒执行')

const config = reactive({
  second: {
    type: 'every',
    start: 0,
    end: 59,
    intervalStart: 0,
    intervalValue: 1,
    values: [] as number[],
  },
  minute: {
    type: 'every',
    start: 0,
    end: 59,
    intervalStart: 0,
    intervalValue: 1,
    values: [] as number[],
  },
  hour: {
    type: 'every',
    start: 0,
    end: 23,
    intervalStart: 0,
    intervalValue: 1,
    values: [] as number[],
  },
  day: {
    type: 'every',
    start: 1,
    end: 31,
    intervalStart: 1,
    intervalValue: 1,
    values: [] as number[],
  },
  month: {
    type: 'every',
    start: 1,
    end: 12,
    intervalStart: 1,
    intervalValue: 1,
    values: [] as number[],
  },
  week: {
    type: 'every',
    values: [] as number[],
    lastValue: 1,
  },
})

const weekOptions = [
  { label: '周日', value: 1 },
  { label: '周一', value: 2 },
  { label: '周二', value: 3 },
  { label: '周三', value: 4 },
  { label: '周四', value: 5 },
  { label: '周五', value: 6 },
  { label: '周六', value: 7 },
]

const presets = {
  everyMinute: '0/1 * * * * ?',
  everyFiveMinutes: '0/5 * * * * ?',
  // 每小时整点执行
  everyHour: '0 0 * * * ?',
  everyDay: '0 0 0 * * ?',
  everyWeek: '0 0 0 ? * 1',
  everyMonth: '0 0 0 1 * ?',
}

const setPreset = (key: keyof typeof presets) => {
  cronExpression.value = presets[key]
  parseExpression(cronExpression.value)
}

const parseExpression = (expr: string) => {
  const parts = expr.split(' ')
  if (parts.length !== 6 && parts.length !== 7) {
    return
  }

  const [second, minute, hour, day, month, week] = parts

  // 解析秒
  if (second === '*') {
    config.second.type = 'every'
  } else if (second.includes('/')) {
    const [start, interval] = second.split('/')
    config.second.type = 'interval'
    config.second.intervalStart = parseInt(start)
    config.second.intervalValue = parseInt(interval)
  } else if (second.includes('-')) {
    const [start, end] = second.split('-')
    config.second.type = 'cycle'
    config.second.start = parseInt(start)
    config.second.end = parseInt(end)
  } else if (second.includes(',')) {
    config.second.type = 'specific'
    config.second.values = second.split(',').map(Number)
  } else {
    config.second.type = 'specific'
    config.second.values = [parseInt(second)]
  }

  // 解析其他字段（类似逻辑）
  // 这里简化处理，实际应该完整解析所有字段
}

const generateExpression = () => {
  let expr = ''

  // 秒
  switch (config.second.type) {
    case 'every':
      expr += '*'
      break
    case 'cycle':
      expr += `${config.second.start}-${config.second.end}`
      break
    case 'interval':
      expr += `${config.second.intervalStart}/${config.second.intervalValue}`
      break
    case 'specific':
      expr += config.second.values.length ? config.second.values.join(',') : '*'
      break
    default:
      expr += '*'
  }
  expr += ' '

  // 分
  switch (config.minute.type) {
    case 'every':
      expr += '*'
      break
    case 'cycle':
      expr += `${config.minute.start}-${config.minute.end}`
      break
    case 'interval':
      expr += `${config.minute.intervalStart}/${config.minute.intervalValue}`
      break
    case 'specific':
      expr += config.minute.values.length ? config.minute.values.join(',') : '*'
      break
    default:
      expr += '*'
  }
  expr += ' '

  // 时
  switch (config.hour.type) {
    case 'every':
      expr += '*'
      break
    case 'cycle':
      expr += `${config.hour.start}-${config.hour.end}`
      break
    case 'interval':
      expr += `${config.hour.intervalStart}/${config.hour.intervalValue}`
      break
    case 'specific':
      expr += config.hour.values.length ? config.hour.values.join(',') : '*'
      break
    default:
      expr += '*'
  }
  expr += ' '

  // 日
  switch (config.day.type) {
    case 'every':
      expr += '*'
      break
    case 'cycle':
      expr += `${config.day.start}-${config.day.end}`
      break
    case 'interval':
      expr += `${config.day.intervalStart}/${config.day.intervalValue}`
      break
    case 'specific':
      expr += config.day.values.length ? config.day.values.join(',') : '*'
      break
    case 'weekday':
      expr += '?'
      break
    case 'last':
      expr += 'L'
      break
    default:
      expr += '*'
  }
  expr += ' '

  // 月
  switch (config.month.type) {
    case 'every':
      expr += '*'
      break
    case 'cycle':
      expr += `${config.month.start}-${config.month.end}`
      break
    case 'interval':
      expr += `${config.month.intervalStart}/${config.month.intervalValue}`
      break
    case 'specific':
      expr += config.month.values.length ? config.month.values.join(',') : '*'
      break
    default:
      expr += '*'
  }
  expr += ' '

  // 周
  switch (config.week.type) {
    case 'every':
      expr += '?'
      break
    case 'specific':
      expr += config.week.values.length ? config.week.values.join(',') : '?'
      break
    case 'last':
      expr += `${config.week.lastValue}L`
      break
    default:
      expr += '?'
  }

  return expr
}

const updateExpression = () => {
  const expr = generateExpression()
  cronExpression.value = expr
  updateDescription()
  calculateNextExecuteTime()
}

const updateDescription = () => {
  const expr = cronExpression.value
  // 简化版描述
  if (expr === '* * * * * ?') {
    expressionDescription.value = '每秒执行'
  } else if (expr === '0 * * * * ?') {
    expressionDescription.value = '每分钟执行'
  } else if (expr === '0/1 * * * * ?') {
    expressionDescription.value = '每秒执行'
  } else if (expr === '0/5 * * * * ?') {
    expressionDescription.value = '每5分钟执行'
  } else if (expr === '0 0 * * * ?') {
    expressionDescription.value = '每小时执行'
  } else if (expr === '0 0 0 * * ?') {
    expressionDescription.value = '每天零点执行'
  } else if (expr === '0 0 0 ? * 1') {
    expressionDescription.value = '每周日零点执行'
  } else if (expr === '0 0 0 1 * ?') {
    expressionDescription.value = '每月1号零点执行'
  } else {
    expressionDescription.value = '自定义执行时间'
  }
}

watch(
  () => props.modelValue,
  (value) => {
    parseExpression(value)
    updateDescription()
  },
  { immediate: true },
)
</script>

<style scoped>
.cron-editor {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 16px;
}

.quick-select {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-weight: 500;
  color: #666;
}

.expression-info {
  font-family: monospace;
}

.expression {
  font-size: 18px;
  font-weight: bold;
  color: #1976d2;
  margin-bottom: 8px;
}

.description {
  color: #666;
  font-size: 14px;
}

.next-time {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.value {
  color: #1976d2;
  font-weight: 500;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inline-input {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 8px;
  margin-top: 8px;
}
</style>
