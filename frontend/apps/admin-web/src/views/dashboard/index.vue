<template>
  <div class="flex flex-col gap-4">
    <NGrid :x-gap="16" :y-gap="16" cols="1 s:2 m:3 l:4 xl:4" responsive="screen">
      <NGi>
        <NCard title="用户总数" class="rounded-xl">
          <div class="flex justify-between items-center">
            <div class="text-3xl font-bold text-blue-500">1,234</div>
            <div class="text-sm text-green-500">较昨日 +12</div>
          </div>
        </NCard>
      </NGi>
      <NGi>
        <NCard title="订单总数" class="rounded-xl">
          <div class="flex justify-between items-center">
            <div class="text-3xl font-bold text-blue-500">5,678</div>
            <div class="text-sm text-green-500">较昨日 +23</div>
          </div>
        </NCard>
      </NGi>
      <NGi>
        <NCard title="销售额" class="rounded-xl">
          <div class="flex justify-between items-center">
            <div class="text-3xl font-bold text-blue-500">¥89,012</div>
            <div class="text-sm text-green-500">较昨日 +5.6%</div>
          </div>
        </NCard>
      </NGi>
      <NGi>
        <NCard title="访问量" class="rounded-xl">
          <div class="flex justify-between items-center">
            <div class="text-3xl font-bold text-blue-500">23,456</div>
            <div class="text-sm text-green-500">较昨日 +8.2%</div>
          </div>
        </NCard>
      </NGi>
    </NGrid>

    <NGrid :x-gap="16" :y-gap="16" cols="1 l:2" responsive="screen">
      <NGi>
        <NCard title="访问趋势" class="h-80">
          <div ref="chartRef" class="h-full"></div>
        </NCard>
      </NGi>
      <NGi>
        <NCard title="最新动态" class="h-80">
          <NTimeline>
            <NTimelineItem v-for="item in activities" :key="item.id" :type="item.type">
              <strong>{{ item.title }}</strong>
              <div class="text-sm text-gray-500">{{ item.time }}</div>
            </NTimelineItem>
          </NTimeline>
        </NCard>
      </NGi>
    </NGrid>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref<HTMLElement | null>(null)

const activities = [
  { id: 1, type: 'success' as const, title: '新用户注册', time: '2026-03-11 16:30' },
  { id: 2, type: 'info' as const, title: '系统更新完成', time: '2026-03-11 15:00' },
  { id: 3, type: 'warning' as const, title: '服务器负载升高', time: '2026-03-11 14:20' },
  { id: 4, type: 'success' as const, title: '订单支付成功', time: '2026-03-11 13:45' },
]

onMounted(() => {
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      },
      yAxis: { type: 'value' },
      series: [
        {
          data: [820, 932, 901, 934, 1290, 1330, 1320],
          type: 'line',
          smooth: true,
          areaStyle: {
            color: 'rgba(24, 144, 255, 0.2)',
          },
          itemStyle: {
            color: '#1890ff',
          },
        },
      ],
    })
  }
})
</script>
