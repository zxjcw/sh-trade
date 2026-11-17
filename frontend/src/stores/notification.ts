import { defineStore } from 'pinia'
import { notificationApi } from '@/api/notification'
import type { Notification } from '@/types'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    items: [] as Notification[],
    loading: false
  }),

  getters: {
    unreadCount: (state) => state.items.filter((item) => !item.read).length
  },

  actions: {
    async load() {
      this.loading = true
      try {
        this.items = await notificationApi.list()
      } finally {
        this.loading = false
      }
    },

    async markRead(id: number) {
      await notificationApi.read(id)
      const target = this.items.find((item) => item.id === id)
      if (target) target.read = true
    },

    async markAllRead() {
      await notificationApi.readAll()
      this.items.forEach((item) => {
        item.read = true
      })
    },

    clear() {
      this.items = []
    }
  }
})
