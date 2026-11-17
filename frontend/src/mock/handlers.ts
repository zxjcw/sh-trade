import type {
  Arbitration,
  AuditRecord,
  Bounty,
  Category,
  ExchangeIntent,
  ExchangePlan,
  Item,
  ItemCondition,
  NotificationType,
  Order,
  PaymentRecord,
  Review,
  RosterEntry,
  User
} from '@/types'
import { maskName } from '@/utils/format'
import { getDb, nextId, saveDb } from './db'

export interface MockRequest {
  method: string
  url: string
  params: Record<string, unknown>
  data?: unknown
  token?: string
}

interface ApiResult<T = unknown> {
  code: number
  message: string
  data?: T
}

class MockError extends Error {
  constructor(
    public code: number,
    message: string
  ) {
    super(message)
  }
}

type Body = Record<string, any>

const db = () => getDb()
const now = () => new Date().toISOString()
const addMinutes = (minutes: number) => new Date(Date.now() + minutes * 60_000).toISOString()
const round = (value: number) => Math.round(value * 100) / 100

function currentUser(token?: string): User | undefined {
  if (!token) return undefined
  const userId = db().sessions[token]
  if (!userId) return undefined
  return db().users.find((user) => user.id === userId)
}

function requireUser(token?: string): User {
  const user = currentUser(token)
  if (!user) throw new MockError(401, '请先登录')
  if (user.banned) throw new MockError(403, '账号已被封禁')
  return user
}

function requireVerified(token?: string): User {
  const user = requireUser(token)
  if (user.verifyStatus !== 'verified') throw new MockError(403, '请先完成学籍认证')
  return user
}

function requireRole(token: string | undefined, roles: User['role'][]): User {
  const user = requireUser(token)
  if (!roles.includes(user.role)) throw new MockError(403, '无权访问该功能')
  return user
}

function findItem(id: number): Item {
  const item = db().items.find((entry) => entry.id === id)
  if (!item) throw new MockError(404, '物品不存在')
  return item
}

function findOrder(id: string): Order {
  const order = db().orders.find((entry) => entry.id === id)
  if (!order) throw new MockError(404, '订单不存在')
  return order
}

function decorateItem(item: Item, user?: User): Item {
  return {
    ...item,
    favorited: user ? db().favorites.some((fav) => fav.userId === user.id && fav.itemId === item.id) : false
  }
}

function notify(
  receiverId: number,
  type: NotificationType,
  title: string,
  content: string,
  link?: string
): void {
  db().notifications.unshift({
    id: nextId(db().notifications),
    receiverId,
    type,
    title,
    content,
    read: false,
    createdAt: now(),
    link
  })
}

function applyBalance(
  user: User,
  delta: number,
  type: PaymentRecord['type'],
  orderId?: string,
  remark?: string
): PaymentRecord {
  user.balance = round(user.balance + delta)
  const record: PaymentRecord = {
    id: nextId(db().paymentRecords),
    orderId,
    accountType: 'user',
    type,
    amount: Math.abs(delta),
    balanceAfter: user.balance,
    externalNo: `EXT${Date.now()}${Math.floor(Math.random() * 900 + 100)}`,
    createdAt: now(),
    remark
  }
  db().paymentRecords.unshift(record)
  return record
}

function addCredit(user: User, change: number, reason: string, orderId?: string): void {
  user.creditScore = Math.max(0, user.creditScore + change)
  db().creditRecords.unshift({
    id: nextId(db().creditRecords),
    change,
    reason,
    orderId,
    createdAt: now()
  })
}

function categoryName(categoryId: number): string {
  return db().categories.find((category) => category.id === categoryId)?.name ?? '未分类'
}

function categoryIds(categoryId: number): number[] {
  const children = db().categories.filter((category) => category.parentId === categoryId).map((c) => c.id)
  return [categoryId, ...children]
}

function orderId(): string {
  const date = new Date()
  const ymd = `${date.getFullYear()}${String(date.getMonth() + 1).padStart(2, '0')}${String(
    date.getDate()
  ).padStart(2, '0')}`
  let seq = db().orders.length + 1
  let id = `SO${ymd}${String(seq).padStart(3, '0')}`
  while (db().orders.some((order) => order.id === id)) {
    seq += 1
    id = `SO${ymd}${String(seq).padStart(3, '0')}`
  }
  return id
}

function matchBounties(item: Item): void {
  for (const bounty of db().bounties) {
    if (bounty.status !== 'open') continue
    if (bounty.categoryId !== item.categoryId) continue
    if (item.price > bounty.budget * 1.2) continue
    bounty.matchCount += 1
    notify(
      bounty.buyerId,
      'bounty_match',
      '悬赏命中',
      `卖家「${item.sellerName}」上架的「${item.title}」符合你的求购条件，去看看？`,
      `/items/${item.id}`
    )
  }
}

function addHistory(
  item: Item,
  type: Item['history'][number]['type'],
  description: string,
  orderNo?: string,
  userName?: string
): void {
  item.history.push({
    id: nextId(item.history),
    type,
    time: now(),
    description,
    orderId: orderNo,
    userName
  })
}

function cancelExpiredOrder(order: Order): void {
  order.tradeStatus = 'cancelled'
  const item = db().items.find((entry) => entry.id === order.itemId)
  if (item && item.status === 'locked') item.status = 'on_sale'
}

function dispatch(req: MockRequest): unknown {
  const url = new URL(req.url, 'http://mock.local')
  const path = url.pathname.replace(/^\/api/, '') || '/'
  const method = req.method.toUpperCase()
  const body = (req.data ?? {}) as Body
  const param = (name: string, fallback = '') =>
    String(req.params[name] ?? url.searchParams.get(name) ?? fallback)
  const user = currentUser(req.token)

  if (method === 'POST' && path === '/auth/login') {
    const username = String(body.username ?? '')
    const password = String(body.password ?? '')
    const target = db().users.find((entry) => entry.username === username)
    if (!target || db().passwords[username] !== password) {
      throw new MockError(400, '用户名或密码错误')
    }
    if (target.banned) throw new MockError(403, '账号已被封禁')
    const token = `mock_${target.id}_${Date.now().toString(36)}`
    db().sessions[token] = target.id
    return { token, user: target }
  }

  if (method === 'POST' && path === '/auth/register') {
    const username = String(body.username ?? '').trim()
    const password = String(body.password ?? '')
    const nickname = String(body.nickname ?? '').trim()
    if (!username || !password || !nickname) throw new MockError(400, '请填写完整信息')
    if (username.length < 3) throw new MockError(400, '用户名至少 3 个字符')
    if (password.length < 6) throw new MockError(400, '密码至少 6 位')
    if (db().users.some((entry) => entry.username === username)) {
      throw new MockError(400, '用户名已存在')
    }
    const created: User = {
      id: nextId(db().users),
      username,
      nickname,
      role: 'student',
      verifyStatus: 'unverified',
      creditScore: 100,
      balance: 0,
      contact: String(body.contact ?? ''),
      createdAt: now()
    }
    db().users.push(created)
    db().passwords[username] = password
    const token = `mock_${created.id}_${Date.now().toString(36)}`
    db().sessions[token] = created.id
    return { token, user: created }
  }

  if (method === 'GET' && path === '/auth/me') {
    return requireUser(req.token)
  }

  if (method === 'POST' && path === '/auth/logout') {
    if (req.token) delete db().sessions[req.token]
    return null
  }

  if (method === 'POST' && path === '/verification') {
    const me = requireUser(req.token)
    if (me.verifyStatus === 'verified') throw new MockError(400, '你已完成学籍认证')
    const studentNo = String(body.studentNo ?? '').trim()
    const name = String(body.name ?? '').trim()
    const entry = db().roster.find((row) => row.studentNo === studentNo)
    if (!entry) throw new MockError(400, '学籍库中未找到该学号')
    if (entry.name !== name) throw new MockError(400, '姓名与学籍信息不一致，请检查后重试')
    if (db().users.some((row) => row.id !== me.id && row.studentNo === studentNo)) {
      throw new MockError(400, '该学号已绑定其他账号')
    }
    me.verifyStatus = 'verified'
    me.studentNo = entry.studentNo
    me.college = entry.college
    me.major = entry.major
    me.grade = entry.grade
    notify(
      me.id,
      'verification',
      '学籍认证通过',
      '欢迎加入拾光集，你已获得发布、下单、求购与置换资格。'
    )
    return me
  }

  if (method === 'GET' && path === '/public/stats') {
    const completed = db().orders.filter((order) => order.tradeStatus === 'completed')
    return {
      verifiedUsers: db().users.filter((entry) => entry.role === 'student' && entry.verifyStatus === 'verified')
        .length,
      onSaleItems: db().items.filter((item) => item.status === 'on_sale').length,
      completedOrders: completed.length,
      totalAmount: round(completed.reduce((sum, order) => sum + order.amount, 0))
    }
  }

  if (method === 'GET' && path === '/categories') {
    return db().categories.filter((category) => category.enabled)
  }

  if (method === 'GET' && path === '/pickup-points') {
    return db().pickupPoints
  }

  if (method === 'GET' && path === '/items') {
    const scope = param('scope')
    const status = param('status')
    const keyword = param('keyword').trim()
    const categoryId = Number(param('categoryId', '0'))
    const condition = param('condition')
    const sort = param('sort', 'latest')
    const limit = Number(param('limit', '0'))

    let list = [...db().items]
    if (scope === 'mine') {
      const me = requireUser(req.token)
      list = list.filter((item) => item.sellerId === me.id)
    } else if (status && status !== 'all') {
      list = list.filter((item) => item.status === status)
    } else if (status === 'all') {
      if (!user || !['operator', 'admin'].includes(user.role)) {
        list = list.filter((item) => item.status === 'on_sale')
      }
    } else {
      list = list.filter((item) => item.status === 'on_sale')
    }

    if (keyword) {
      const lower = keyword.toLowerCase()
      list = list.filter(
        (item) =>
          item.title.toLowerCase().includes(lower) || item.description.toLowerCase().includes(lower)
      )
    }
    if (categoryId) {
      const ids = categoryIds(categoryId)
      list = list.filter((item) => ids.includes(item.categoryId))
    }
    if (condition) list = list.filter((item) => item.condition === condition)

    if (sort === 'price_asc') list.sort((a, b) => a.price - b.price)
    else if (sort === 'price_desc') list.sort((a, b) => b.price - a.price)
    else list.sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))

    if (limit > 0) list = list.slice(0, limit)
    return list.map((item) => decorateItem(item, user))
  }

  const itemMatch = path.match(/^\/items\/(\d+)$/)
  if (itemMatch && method === 'GET') {
    const item = findItem(Number(itemMatch[1]))
    const relatedOrder = db().orders.some(
      (order) =>
        order.itemId === item.id &&
        user &&
        (order.buyerId === user.id || order.sellerId === user.id) &&
        order.tradeStatus !== 'cancelled'
    )
    if (
      item.status !== 'on_sale' &&
      item.sellerId !== user?.id &&
      !relatedOrder &&
      !['operator', 'admin'].includes(user?.role ?? '')
    ) {
      throw new MockError(404, '物品不存在或已下架')
    }
    return decorateItem(item, user)
  }

  if (method === 'POST' && path === '/items') {
    const me = requireVerified(req.token)
    const title = String(body.title ?? '').trim()
    const price = Number(body.price)
    const categoryId = Number(body.categoryId)
    if (!title) throw new MockError(400, '请填写物品标题')
    if (!categoryId || !db().categories.some((category) => category.id === categoryId)) {
      throw new MockError(400, '请选择分类')
    }
    if (!(price > 0)) throw new MockError(400, '请填写有效价格')
    const coverSeed = Number(body.coverSeed) || Math.floor(Math.random() * 80) + 20
    const item: Item = {
      id: nextId(db().items),
      title,
      categoryId,
      categoryName: categoryName(categoryId),
      price,
      originalPrice: Number(body.originalPrice) || price,
      purchasePrice: Number(body.purchasePrice) || undefined,
      condition: (body.condition as ItemCondition) || 'good',
      description: String(body.description ?? ''),
      status: 'pending',
      source: body.source === 'platform' ? 'platform' : 'offline',
      notesTag: body.notesTag ?? 'no_notes',
      sellerId: me.id,
      sellerName: me.nickname,
      sellerCredit: me.creditScore,
      coverSeed,
      images: [
        { id: coverSeed * 10 + 1, seed: coverSeed, isCover: true, status: 'active' },
        { id: coverSeed * 10 + 2, seed: coverSeed + 2, isCover: false, status: 'active' },
        { id: coverSeed * 10 + 3, seed: coverSeed + 4, isCover: false, status: 'active' }
      ],
      history: [
        {
          id: 1,
          type: 'created',
          time: now(),
          description: '物品建档，等待第一位主人'
        }
      ],
      createdAt: now(),
      updatedAt: now()
    }
    db().items.unshift(item)
    return item
  }

  const relistMatch = path.match(/^\/items\/(\d+)\/relist$/)
  if (method === 'POST' && relistMatch) {
    const me = requireVerified(req.token)
    const item = findItem(Number(relistMatch[1]))
    if (item.sellerId !== me.id) throw new MockError(403, '只有当前持有人可以再次上架')
    if (item.status !== 'completed') throw new MockError(400, '只有已完成的物品可以再次上架')
    const sold = db().orders.some(
      (order) =>
        order.itemId === item.id &&
        order.buyerId === me.id &&
        order.tradeStatus === 'completed'
    )
    if (!sold) throw new MockError(403, '没有查询到你的购买记录')
    item.price = Number(body.price) || item.price
    item.condition = (body.condition as ItemCondition) || item.condition
    if (body.description) item.description = String(body.description)
    item.coverSeed = Number(body.coverSeed) || item.coverSeed
    item.images = item.images.map((image) => ({ ...image, status: 'history' as const }))
    item.images.push(
      { id: Date.now(), seed: item.coverSeed, isCover: true, status: 'active' },
      { id: Date.now() + 1, seed: item.coverSeed + 2, isCover: false, status: 'active' }
    )
    item.status = 'pending'
    item.updatedAt = now()
    return item
  }

  const favoriteMatch = path.match(/^\/items\/(\d+)\/favorite$/)
  if (method === 'POST' && favoriteMatch) {
    const me = requireUser(req.token)
    const itemId = Number(favoriteMatch[1])
    findItem(itemId)
    const index = db().favorites.findIndex((fav) => fav.userId === me.id && fav.itemId === itemId)
    if (index >= 0) {
      db().favorites.splice(index, 1)
      return { favorited: false }
    }
    db().favorites.push({ userId: me.id, itemId })
    return { favorited: true }
  }

  if (method === 'GET' && path === '/favorites') {
    const me = requireUser(req.token)
    const ids = db()
      .favorites.filter((fav) => fav.userId === me.id)
      .map((fav) => fav.itemId)
    return db()
      .items.filter((item) => ids.includes(item.id))
      .map((item) => decorateItem(item, me))
  }

  if (method === 'GET' && path === '/orders') {
    const me = requireUser(req.token)
    const role = param('role', 'buyer')
    const status = param('status')
    let list = db().orders.filter((order) => {
      if (role === 'seller') return order.sellerId === me.id
      if (role === 'all') return order.buyerId === me.id || order.sellerId === me.id
      return order.buyerId === me.id
    })
    if (status && status !== 'all') list = list.filter((order) => order.tradeStatus === status)
    return list.sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
  }

  const orderMatch = path.match(/^\/orders\/([^/]+)$/)
  if (method === 'GET' && orderMatch) {
    const me = requireUser(req.token)
    const order = findOrder(orderMatch[1])
    if (order.buyerId !== me.id && order.sellerId !== me.id && !['operator', 'admin'].includes(me.role)) {
      throw new MockError(403, '无权查看该订单')
    }
    return order
  }

  if (method === 'POST' && path === '/orders') {
    const me = requireVerified(req.token)
    const item = findItem(Number(body.itemId))
    if (item.status !== 'on_sale') throw new MockError(400, '该物品当前不可购买')
    if (item.sellerId === me.id) throw new MockError(400, '不能购买自己发布的物品')
    const deliveryMethod = body.deliveryMethod === 'pickup' ? 'pickup' : 'meetup'
    let pickupPointName: string | undefined
    if (deliveryMethod === 'pickup') {
      const point = db().pickupPoints.find((entry) => entry.id === Number(body.pickupPointId))
      if (!point) throw new MockError(400, '请选择自提点')
      pickupPointName = point.name
    }
    const order: Order = {
      id: orderId(),
      type: 'normal',
      itemId: item.id,
      itemTitle: item.title,
      itemCoverSeed: item.coverSeed,
      itemCondition: item.condition,
      buyerId: me.id,
      buyerName: me.nickname,
      sellerId: item.sellerId,
      sellerName: item.sellerName,
      amount: item.price,
      deliveryMethod,
      pickupPointId: deliveryMethod === 'pickup' ? Number(body.pickupPointId) : undefined,
      pickupPointName,
      tradeStatus: 'pending_payment',
      afterSaleStatus: 'none',
      createdAt: now(),
      payDeadline: addMinutes(2)
    }
    db().orders.unshift(order)
    item.status = 'locked'
    return order
  }

  const payMatch = path.match(/^\/orders\/([^/]+)\/pay$/)
  if (method === 'POST' && payMatch) {
    const me = requireUser(req.token)
    const order = findOrder(payMatch[1])
    if (order.buyerId !== me.id) throw new MockError(403, '无权支付该订单')
    if (order.tradeStatus !== 'pending_payment') throw new MockError(400, '订单当前不可支付')
    if (order.payDeadline && +new Date(order.payDeadline) < Date.now()) {
      cancelExpiredOrder(order)
      throw new MockError(400, '订单已超时取消')
    }
    if (me.balance < order.amount) throw new MockError(400, '余额不足，请先充值')
    applyBalance(me, -order.amount, 'payment', order.id, '订单支付')
    order.tradeStatus = 'pending_delivery'
    order.paidAt = now()
    order.payDeadline = undefined
    return order
  }

  const cancelMatch = path.match(/^\/orders\/([^/]+)\/cancel$/)
  if (method === 'POST' && cancelMatch) {
    const me = requireUser(req.token)
    const order = findOrder(cancelMatch[1])
    if (order.buyerId !== me.id) throw new MockError(403, '无权操作该订单')
    if (order.tradeStatus !== 'pending_payment') throw new MockError(400, '订单当前不可取消')
    cancelExpiredOrder(order)
    return order
  }

  const confirmMatch = path.match(/^\/orders\/([^/]+)\/confirm$/)
  if (method === 'POST' && confirmMatch) {
    const me = requireUser(req.token)
    const order = findOrder(confirmMatch[1])
    if (order.buyerId !== me.id) throw new MockError(403, '只有买家可以确认收货')
    if (order.tradeStatus !== 'pending_confirm') throw new MockError(400, '订单当前不可确认')
    const seller = db().users.find((entry) => entry.id === order.sellerId)
    order.tradeStatus = 'completed'
    order.completedAt = now()
    order.confirmDeadline = undefined
    const item = db().items.find((entry) => entry.id === order.itemId)
    if (item) {
      item.status = 'completed'
      addHistory(item, 'deal', `成交，新主人：${me.nickname}`, order.id, me.nickname)
      item.sellerId = me.id
      item.sellerName = me.nickname
      item.sellerCredit = me.creditScore
    }
    if (seller) {
      applyBalance(seller, order.amount, 'payout', order.id, '订单放款')
      addCredit(seller, 2, '完成交易', order.id)
      notify(
        seller.id,
        'order_completed',
        '订单已完成',
        `「${order.itemTitle}」已确认收货，${order.amount} 元已放款到你的钱包。`,
        `/orders/${order.id}`
      )
    }
    addCredit(me, 2, '完成交易', order.id)
    notify(
      me.id,
      'order_completed',
      '订单已完成',
      `「${order.itemTitle}」交易完成，双方信用 +2。`,
      `/orders/${order.id}`
    )
    if (order.bountyId) {
      const bounty = db().bounties.find((entry) => entry.id === order.bountyId)
      if (bounty) bounty.status = 'matched'
    }
    return order
  }

  const reviewMatch = path.match(/^\/orders\/([^/]+)\/review$/)
  if (method === 'POST' && reviewMatch) {
    const me = requireUser(req.token)
    const order = findOrder(reviewMatch[1])
    if (order.buyerId !== me.id && order.sellerId !== me.id) throw new MockError(403, '无权评价该订单')
    if (order.tradeStatus !== 'completed') throw new MockError(400, '订单完成后才能评价')
    if (db().reviews.some((review) => review.orderId === order.id && review.reviewerId === me.id)) {
      throw new MockError(400, '你已经评价过该订单')
    }
    const isBuyer = order.buyerId === me.id
    const review: Review = {
      id: nextId(db().reviews),
      orderId: order.id,
      itemTitle: order.itemTitle,
      reviewerId: me.id,
      reviewerName: me.nickname,
      targetName: isBuyer ? order.sellerName : order.buyerName,
      rating: Number(body.rating) || 5,
      tags: Array.isArray(body.tags) ? body.tags : [],
      content: String(body.content ?? ''),
      status: 'valid',
      createdAt: now()
    }
    db().reviews.unshift(review)
    order.reviewed = true
    return review
  }

  const aftersaleMatch = path.match(/^\/orders\/([^/]+)\/aftersale$/)
  if (method === 'POST' && aftersaleMatch) {
    const me = requireUser(req.token)
    const order = findOrder(aftersaleMatch[1])
    if (order.buyerId !== me.id) throw new MockError(403, '只有买家可以申请售后')
    if (order.tradeStatus === 'cancelled') throw new MockError(400, '已取消的订单不能申请售后')
    if (order.tradeStatus === 'pending_payment') throw new MockError(400, '订单未支付，不能申请售后')
    if (order.afterSaleStatus !== 'none') throw new MockError(400, '该订单已有售后处理中')
    if (
      order.tradeStatus === 'completed' &&
      order.completedAt &&
      Date.now() - +new Date(order.completedAt) > 5 * 60_000
    ) {
      throw new MockError(400, '已超过收货后 5 分钟售后窗口')
    }
    const reason = String(body.reason ?? '').trim()
    if (!reason) throw new MockError(400, '请填写退款原因')

    if (order.tradeStatus === 'pending_delivery') {
      order.afterSaleStatus = 'refunded'
      applyBalance(me, order.amount, 'refund', order.id, '未发货退款')
      const item = db().items.find((entry) => entry.id === order.itemId)
      if (item) item.status = 'on_sale'
      notify(me.id, 'aftersale_result', '退款成功', `订单 ${order.id} 已原路退回 ${order.amount} 元。`, `/orders/${order.id}`)
      notify(order.sellerId, 'aftersale_result', '订单已退款', `订单 ${order.id} 已退款，物品已重新上架。`, `/orders/${order.id}`)
    } else {
      order.afterSaleStatus = 'arbitrating'
      const arbitration: Arbitration = {
        id: nextId(db().arbitrations),
        orderId: order.id,
        applicantName: order.buyerName,
        targetName: order.sellerName,
        type: reason,
        evidence: String(body.evidence ?? ''),
        status: 'pending',
        createdAt: now()
      }
      db().arbitrations.unshift(arbitration)
      notify(me.id, 'arbitration_accepted', '售后已转仲裁', '卖家未在时限内处理，运营已受理你的售后申请。', `/orders/${order.id}`)
      notify(order.sellerId, 'arbitration_accepted', '收到售后仲裁', `订单 ${order.id} 的售后已转运营仲裁。`, `/orders/${order.id}`)
    }
    return order
  }

  if (method === 'GET' && path === '/wallet') {
    const me = requireUser(req.token)
    return { balance: me.balance }
  }

  if (method === 'POST' && path === '/wallet/recharge') {
    const me = requireUser(req.token)
    const amount = Number(body.amount)
    if (!(amount > 0)) throw new MockError(400, '请输入有效金额')
    if (amount > 5000) throw new MockError(400, '单次充值不能超过 5000 元')
    const record = applyBalance(me, amount, 'recharge', undefined, '钱包充值')
    return { balance: me.balance, record }
  }

  if (method === 'GET' && path === '/payments') {
    const me = requireUser(req.token)
    return db()
      .paymentRecords.filter((record) => record.accountType === 'user')
      .filter((record) => {
        if (!record.orderId) return record.type === 'recharge'
        const order = db().orders.find((entry) => entry.id === record.orderId)
        return order ? order.buyerId === me.id || order.sellerId === me.id : false
      })
      .sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
  }

  if (method === 'GET' && path === '/notifications') {
    const me = requireUser(req.token)
    return db()
      .notifications.filter((notification) => notification.receiverId === me.id)
      .sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
  }

  const readOneMatch = path.match(/^\/notifications\/(\d+)\/read$/)
  if (method === 'POST' && readOneMatch) {
    const me = requireUser(req.token)
    const notification = db().notifications.find(
      (entry) => entry.id === Number(readOneMatch[1]) && entry.receiverId === me.id
    )
    if (!notification) throw new MockError(404, '通知不存在')
    notification.read = true
    return notification
  }

  if (method === 'POST' && path === '/notifications/read-all') {
    const me = requireUser(req.token)
    db()
      .notifications.filter((entry) => entry.receiverId === me.id)
      .forEach((entry) => {
        entry.read = true
      })
    return null
  }

  if (method === 'GET' && path === '/reviews/me') {
    const me = requireUser(req.token)
    return db().reviews.filter(
      (review) => review.reviewerId === me.id || review.targetName === me.nickname
    )
  }

  if (method === 'GET' && path === '/credit/records') {
    const me = requireUser(req.token)
    return db()
      .creditRecords.filter((record) => {
        if (!record.orderId) return true
        const order = db().orders.find((entry) => entry.id === record.orderId)
        return order ? order.buyerId === me.id || order.sellerId === me.id : false
      })
      .sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
  }

  if (method === 'GET' && path === '/bounties') {
    const me = user
    const scope = param('scope', 'hall')
    if (scope === 'mine') {
      const mine = requireUser(req.token)
      return db().bounties.filter((bounty) => bounty.buyerId === mine.id)
    }
    return db()
      .bounties.filter((bounty) => bounty.status === 'open')
      .map((bounty) => ({ ...bounty, buyerName: me?.id === bounty.buyerId ? bounty.buyerName : maskName(bounty.buyerName) }))
  }

  if (method === 'POST' && path === '/bounties') {
    const me = requireVerified(req.token)
    if (db().bounties.filter((bounty) => bounty.buyerId === me.id && bounty.status === 'open').length >= 3) {
      throw new MockError(400, '同时最多发布 3 条悬赏')
    }
    const categoryId = Number(body.categoryId)
    const keyword = String(body.keyword ?? '').trim()
    const budget = Number(body.budget)
    if (!categoryId || !keyword) throw new MockError(400, '请填写分类和关键词')
    if (!(budget > 0)) throw new MockError(400, '请填写有效预算')
    const bounty: Bounty = {
      id: nextId(db().bounties),
      buyerId: me.id,
      buyerName: me.nickname,
      categoryId,
      categoryName: categoryName(categoryId),
      keyword,
      budget,
      condition: (body.condition as ItemCondition) || 'good',
      status: 'open',
      createdAt: now(),
      expiresAt: addMinutes(60 * 24 * Number(body.expiresDays || 7)),
      matchCount: 0
    }
    db().bounties.unshift(bounty)
    return bounty
  }

  const bountyCloseMatch = path.match(/^\/bounties\/(\d+)\/close$/)
  if (method === 'POST' && bountyCloseMatch) {
    const me = requireUser(req.token)
    const bounty = db().bounties.find((entry) => entry.id === Number(bountyCloseMatch[1]))
    if (!bounty) throw new MockError(404, '悬赏不存在')
    if (bounty.buyerId !== me.id) throw new MockError(403, '无权关闭该悬赏')
    if (bounty.status !== 'open') throw new MockError(400, '该悬赏当前不可关闭')
    bounty.status = 'closed'
    return bounty
  }

  if (method === 'GET' && path === '/exchange/intents') {
    const me = requireUser(req.token)
    return db().intents.filter((intent) => intent.userId === me.id)
  }

  if (method === 'POST' && path === '/exchange/intents') {
    const me = requireVerified(req.token)
    const myItem = findItem(Number(body.myItemId))
    if (myItem.sellerId !== me.id) throw new MockError(403, '只能用自己的物品发起置换')
    if (myItem.status !== 'on_sale') throw new MockError(400, '该物品当前不可用于置换')
    const matchType = body.matchType === 'condition' ? 'condition' : 'specific'
    if (matchType === 'specific') {
      const target = findItem(Number(body.targetItemId))
      if (target.id === myItem.id) throw new MockError(400, '不能和自己置换')
    } else if (!Number(body.targetCategoryId) && !String(body.keyword ?? '').trim()) {
      throw new MockError(400, '请填写期望分类或关键词')
    }
    const intent: ExchangeIntent = {
      id: nextId(db().intents),
      userId: me.id,
      userName: me.nickname,
      myItemId: myItem.id,
      myItemTitle: myItem.title,
      myItemSeed: myItem.coverSeed,
      matchType,
      targetItemId: matchType === 'specific' ? Number(body.targetItemId) : undefined,
      targetItemTitle:
        matchType === 'specific'
          ? db().items.find((item) => item.id === Number(body.targetItemId))?.title
          : undefined,
      targetCategoryId: matchType === 'condition' ? Number(body.targetCategoryId) : undefined,
      targetCategoryName:
        matchType === 'condition' ? categoryName(Number(body.targetCategoryId)) : undefined,
      keyword: matchType === 'condition' ? String(body.keyword ?? '') : undefined,
      status: 'active',
      createdAt: now()
    }
    db().intents.unshift(intent)
    return intent
  }

  if (method === 'GET' && path === '/exchange/plans') {
    const me = requireUser(req.token)
    return db().plans.filter((plan) => plan.participants.some((entry) => entry.userId === me.id))
  }

  const planConfirmMatch = path.match(/^\/exchange\/plans\/(\d+)\/confirm$/)
  if (method === 'POST' && planConfirmMatch) {
    const me = requireUser(req.token)
    const plan = db().plans.find((entry) => entry.id === Number(planConfirmMatch[1]))
    if (!plan) throw new MockError(404, '置换方案不存在')
    const participant = plan.participants.find((entry) => entry.userId === me.id)
    if (!participant) throw new MockError(403, '你不是该方案参与人')
    if (plan.status !== 'pending') throw new MockError(400, '方案当前不可确认')
    participant.confirmed = true
    if (plan.participants.every((entry) => entry.confirmed)) {
      plan.status = 'active'
      for (const entry of plan.participants) {
        notify(
          entry.userId,
          'exchange_invite',
          '置换方案已生效',
          '三方已全部确认，请按约定完成线下面交。',
          '/exchange'
        )
      }
    }
    return plan
  }

  const planRejectMatch = path.match(/^\/exchange\/plans\/(\d+)\/reject$/)
  if (method === 'POST' && planRejectMatch) {
    const me = requireUser(req.token)
    const plan = db().plans.find((entry) => entry.id === Number(planRejectMatch[1]))
    if (!plan) throw new MockError(404, '置换方案不存在')
    if (!plan.participants.some((entry) => entry.userId === me.id)) {
      throw new MockError(403, '你不是该方案参与人')
    }
    plan.status = 'void'
    for (const entry of plan.participants) {
      const item = db().items.find((candidate) => candidate.title === entry.itemTitle && candidate.status === 'locked')
      if (item) item.status = 'on_sale'
      notify(entry.userId, 'exchange_invite', '置换方案作废', '有参与人拒绝了本次置换，物品已解锁。', '/exchange')
    }
    return plan
  }

  if (method === 'GET' && path === '/ops/items/pending') {
    requireRole(req.token, ['operator', 'admin'])
    return db()
      .items.filter((item) => item.status === 'pending')
      .sort((a, b) => +new Date(a.createdAt) - +new Date(b.createdAt))
  }

  const auditMatch = path.match(/^\/ops\/items\/(\d+)\/audit$/)
  if (method === 'POST' && auditMatch) {
    const me = requireRole(req.token, ['operator', 'admin'])
    const item = findItem(Number(auditMatch[1]))
    if (item.status !== 'pending') throw new MockError(400, '该物品不在待审核状态')
    const result = body.result === 'approved' ? 'approved' : 'rejected'
    item.status = result === 'approved' ? 'on_sale' : 'rejected'
    item.updatedAt = now()
    item.rejectReason = result === 'rejected' ? String(body.reason ?? '信息不符合上架要求') : undefined
    const record: AuditRecord = {
      id: nextId(db().auditRecords),
      itemId: item.id,
      itemTitle: item.title,
      result,
      reason: item.rejectReason,
      operator: me.nickname,
      createdAt: now()
    }
    db().auditRecords.unshift(record)
    notify(
      item.sellerId,
      'audit',
      result === 'approved' ? '物品审核通过' : '物品审核驳回',
      result === 'approved'
        ? `你发布的「${item.title}」已通过审核并上架。`
        : `你发布的「${item.title}」被驳回：${item.rejectReason}`,
      `/items/${item.id}`
    )
    if (result === 'approved') matchBounties(item)
    return item
  }

  if (method === 'GET' && path === '/ops/arbitrations') {
    requireRole(req.token, ['operator', 'admin'])
    return [...db().arbitrations].sort((a, b) => {
      if (a.status !== b.status) return a.status === 'pending' ? -1 : 1
      return +new Date(b.createdAt) - +new Date(a.createdAt)
    })
  }

  const resolveMatch = path.match(/^\/ops\/arbitrations\/(\d+)\/resolve$/)
  if (method === 'POST' && resolveMatch) {
    const me = requireRole(req.token, ['operator', 'admin'])
    const arbitration = db().arbitrations.find((entry) => entry.id === Number(resolveMatch[1]))
    if (!arbitration) throw new MockError(404, '仲裁申请不存在')
    if (arbitration.status !== 'pending') throw new MockError(400, '该仲裁已处理')
    const order = findOrder(arbitration.orderId)
    const result = body.result === 'buyer' ? 'buyer' : 'seller'
    arbitration.status = 'resolved'
    arbitration.result = result
    arbitration.handler = me.nickname
    arbitration.resolvedAt = now()
    if (result === 'buyer') {
      const buyer = db().users.find((entry) => entry.id === order.buyerId)
      const seller = db().users.find((entry) => entry.id === order.sellerId)
      if (buyer) applyBalance(buyer, order.amount, 'refund', order.id, '仲裁退款')
      if (seller && order.tradeStatus === 'completed') {
        applyBalance(seller, -order.amount, 'chargeback', order.id, '仲裁扣回')
        addCredit(seller, -2, '售后回滚', order.id)
      }
      order.afterSaleStatus = 'refunded'
      const item = db().items.find((entry) => entry.id === order.itemId)
      if (item) {
        item.status = 'on_sale'
        item.sellerId = order.sellerId
        item.sellerName = order.sellerName
        addHistory(item, 'return', '售后成立，物品退回并重新上架', order.id)
      }
      db()
        .reviews.filter((review) => review.orderId === order.id)
        .forEach((review) => {
          review.status = 'void'
        })
      notify(buyer?.id ?? order.buyerId, 'arbitration_result', '仲裁结果：支持买家', `订单 ${order.id} 已退款。`, `/orders/${order.id}`)
      notify(order.sellerId, 'arbitration_result', '仲裁结果：支持买家', `订单 ${order.id} 已退款并扣回货款。`, `/orders/${order.id}`)
    } else {
      order.afterSaleStatus = 'none'
      notify(order.buyerId, 'arbitration_result', '仲裁结果：支持卖家', `订单 ${order.id} 的售后申请被驳回。`, `/orders/${order.id}`)
      notify(order.sellerId, 'arbitration_result', '仲裁结果：支持卖家', `订单 ${order.id} 的售后申请已驳回。`, `/orders/${order.id}`)
    }
    return arbitration
  }

  if (method === 'GET' && path === '/ops/report') {
    requireRole(req.token, ['operator', 'admin'])
    const completed = db().orders.filter((order) => order.tradeStatus === 'completed')
    const paid = db().orders.filter((order) =>
      ['pending_delivery', 'pending_confirm', 'completed'].includes(order.tradeStatus)
    )
    const refunded = db().orders.filter((order) => order.afterSaleStatus === 'refunded')
    const categoryMap = new Map<string, { count: number; amount: number }>()
    for (const order of completed) {
      const item = db().items.find((entry) => entry.id === order.itemId)
      const name = item?.categoryName ?? '其他'
      const current = categoryMap.get(name) ?? { count: 0, amount: 0 }
      current.count += 1
      current.amount = round(current.amount + order.amount)
      categoryMap.set(name, current)
    }
    const monthlyStats = Array.from({ length: 6 }).map((_, index) => {
      const date = new Date()
      date.setMonth(date.getMonth() - (5 - index))
      const month = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
      const orders = completed.filter((order) =>
        (order.completedAt ?? order.createdAt).startsWith(month)
      )
      return {
        month,
        count: orders.length,
        amount: round(orders.reduce((sum, order) => sum + order.amount, 0))
      }
    })
    return {
      completedCount: completed.length,
      completedAmount: round(completed.reduce((sum, order) => sum + order.amount, 0)),
      refundRate: paid.length ? round((refunded.length / paid.length) * 100) : 0,
      pendingAudits: db().items.filter((item) => item.status === 'pending').length,
      pendingArbitrations: db().arbitrations.filter((entry) => entry.status === 'pending').length,
      categoryStats: [...categoryMap.entries()]
        .map(([name, value]) => ({ name, ...value }))
        .sort((a, b) => b.count - a.count),
      monthlyStats
    }
  }

  if (method === 'GET' && path === '/admin/roster') {
    requireRole(req.token, ['admin'])
    const keyword = param('keyword').trim()
    if (!keyword) return db().roster
    return db().roster.filter(
      (row) =>
        row.studentNo.includes(keyword) ||
        row.name.includes(keyword) ||
        row.college.includes(keyword) ||
        row.major.includes(keyword)
    )
  }

  if (method === 'POST' && path === '/admin/roster/import') {
    requireRole(req.token, ['admin'])
    const rows = Array.isArray(body.rows) ? (body.rows as RosterEntry[]) : []
    if (!rows.length) throw new MockError(400, '没有可导入的数据')
    for (const row of rows) {
      if (!row.studentNo || !row.name) continue
      const index = db().roster.findIndex((entry) => entry.studentNo === row.studentNo)
      if (index >= 0) db().roster[index] = { ...db().roster[index], ...row }
      else db().roster.push(row)
    }
    return { count: rows.length }
  }

  if (method === 'GET' && path === '/admin/categories') {
    requireRole(req.token, ['admin'])
    const roots: Category[] = db()
      .categories.filter((category) => category.parentId === null)
      .map((category) => ({
        ...category,
        children: db().categories.filter((child) => child.parentId === category.id)
      }))
    return roots
  }

  if (method === 'POST' && path === '/admin/categories') {
    requireRole(req.token, ['admin'])
    const name = String(body.name ?? '').trim()
    const parentId = body.parentId ? Number(body.parentId) : null
    if (!name) throw new MockError(400, '请填写分类名')
    if (parentId) {
      const parent = db().categories.find((category) => category.id === parentId)
      if (!parent) throw new MockError(400, '父分类不存在')
      if (parent.parentId !== null) throw new MockError(400, '分类最多两级')
    }
    if (db().categories.some((category) => category.parentId === parentId && category.name === name)) {
      throw new MockError(400, '同级分类不能重名')
    }
    const category: Category = { id: nextId(db().categories), name, parentId, enabled: true }
    db().categories.push(category)
    return category
  }

  const categoryMatch = path.match(/^\/admin\/categories\/(\d+)$/)
  if (method === 'PUT' && categoryMatch) {
    requireRole(req.token, ['admin'])
    const category = db().categories.find((entry) => entry.id === Number(categoryMatch[1]))
    if (!category) throw new MockError(404, '分类不存在')
    if (body.name) category.name = String(body.name)
    if (typeof body.enabled === 'boolean') category.enabled = body.enabled
    return category
  }
  if (method === 'DELETE' && categoryMatch) {
    requireRole(req.token, ['admin'])
    const id = Number(categoryMatch[1])
    if (db().categories.some((category) => category.parentId === id)) {
      throw new MockError(400, '存在子分类，不能删除')
    }
    if (db().items.some((item) => item.categoryId === id)) {
      throw new MockError(400, '分类已被物品引用，不能删除')
    }
    const index = db().categories.findIndex((category) => category.id === id)
    if (index < 0) throw new MockError(404, '分类不存在')
    db().categories.splice(index, 1)
    return null
  }

  if (method === 'GET' && path === '/admin/pickup-points') {
    requireRole(req.token, ['admin'])
    return db().pickupPoints
  }

  if (method === 'POST' && path === '/admin/pickup-points') {
    requireRole(req.token, ['admin'])
    const name = String(body.name ?? '').trim()
    const address = String(body.address ?? '').trim()
    if (!name || !address) throw new MockError(400, '请填写名称和地址')
    const point = { id: nextId(db().pickupPoints), name, address }
    db().pickupPoints.push(point)
    return point
  }

  const pickupMatch = path.match(/^\/admin\/pickup-points\/(\d+)$/)
  if (method === 'PUT' && pickupMatch) {
    requireRole(req.token, ['admin'])
    const point = db().pickupPoints.find((entry) => entry.id === Number(pickupMatch[1]))
    if (!point) throw new MockError(404, '自提点不存在')
    if (body.name) point.name = String(body.name)
    if (body.address) point.address = String(body.address)
    return point
  }
  if (method === 'DELETE' && pickupMatch) {
    requireRole(req.token, ['admin'])
    const id = Number(pickupMatch[1])
    if (db().orders.some((order) => order.pickupPointId === id)) {
      throw new MockError(400, '自提点已被订单引用，不能删除')
    }
    const index = db().pickupPoints.findIndex((point) => point.id === id)
    if (index < 0) throw new MockError(404, '自提点不存在')
    db().pickupPoints.splice(index, 1)
    return null
  }

  if (method === 'GET' && path === '/admin/operators') {
    requireRole(req.token, ['admin'])
    return db().users.filter((entry) => entry.role === 'operator')
  }

  if (method === 'POST' && path === '/admin/operators') {
    requireRole(req.token, ['admin'])
    const username = String(body.username ?? '').trim()
    const nickname = String(body.nickname ?? '').trim()
    const password = String(body.password ?? '')
    if (!username || !nickname || password.length < 6) {
      throw new MockError(400, '请填写用户名、昵称和至少 6 位密码')
    }
    if (db().users.some((entry) => entry.username === username)) {
      throw new MockError(400, '用户名已存在')
    }
    const operator: User = {
      id: nextId(db().users),
      username,
      nickname,
      role: 'operator',
      verifyStatus: 'verified',
      creditScore: 100,
      balance: 0,
      createdAt: now()
    }
    db().users.push(operator)
    db().passwords[username] = password
    return operator
  }

  throw new MockError(404, `未实现的模拟接口：${method} ${path}`)
}

export function handleMockRequest(req: MockRequest): ApiResult {
  try {
    const data = dispatch(req)
    if (req.method.toUpperCase() !== 'GET') saveDb()
    return { code: 0, message: 'ok', data }
  } catch (error) {
    if (error instanceof MockError) {
      return { code: error.code, message: error.message }
    }
    console.error('[mock]', error)
    return { code: 500, message: '模拟接口异常，请重置演示数据后重试' }
  }
}
