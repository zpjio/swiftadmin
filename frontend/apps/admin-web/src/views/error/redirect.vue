<template>
  <div></div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  const { params, query } = route
  const { path } = params
  if (path) {
    router.replace({
      path: '/' + path,
      query
    })
  } else {
    router.push('/')
  }
})
</script>
