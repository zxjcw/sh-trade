export type Role = 'student' | 'operator' | 'admin'
export type VerifyStatus = 'unverified' | 'verified' | 'rejected'

export type ItemCondition = 'new' | 'like_new' | 'good' | 'fair' | 'poor'
export type ItemStatus = 'pending' | 'on_sale' | 'locked' | 'completed' | 'rejected' | 'off_shelf' | 'frozen'
export type ItemSource = 'platform' | 'offline'
export type NotesTag = 'with_notes' | 'with_answers' | 'no_notes'

export type TradeStatus = 'pending_payment' | 'pending_delivery' | 'pending_confirm' | 'completed' | 'cancelled'
export type AfterSaleStatus = 'none' | 'refunding' | 'arbitrating' | 'refunded'
export type DeliveryMethod = 'meetup' | 'pickup'
export type OrderType = 'normal' | 'exchange'

export type BountyStatus = 'open' | 'matched' | 'closed' | 'expired'
export type IntentStatus = 'active' | 'matched' | 'cancelled'
export type PlanStatus = 'pending' | 'active' | 'void' | 'completed'
export type ArbitrationStatus = 'pending' | 'resolved'

export type NotificationType =
  | 'verification'
  | 'audit'
  | 'bounty_match'
  | 'exchange_invite'
  | 'aftersale_result'
  | 'arbitration_accepted'
  | 'arbitration_result'
  | 'order_completed'

export interface User {
  id: number
  username: string
  nickname: string
  role: Role
  verifyStatus: VerifyStatus
  creditScore: number
  balance: number
  studentNo?: string
  college?: string
  major?: string
  grade?: string
  contact?: string
  banned?: boolean
  createdAt: string
}

export interface Category {
  id: number
  name: string
  parentId: number | null
  enabled: boolean
  children?: Category[]
}

export interface PickupPoint {
  id: number
  name: string
  address: string
}

export interface ItemImage {
  id: number
  seed: number
  isCover: boolean
  status: 'active' | 'history'
}

export interface HistoryEvent {
  id: number
  type: 'created' | 'deal' | 'exchange' | 'return' | 'repair'
  time: string
  description: string
  orderId?: string
  userName?: string
}

export interface Item {
  id: number
  title: string
  categoryId: number
  categoryName: string
  price: number
  originalPrice: number
  purchasePrice?: number
  condition: ItemCondition
  description: string
  status: ItemStatus
  source: ItemSource
  notesTag: NotesTag
  sellerId: number
  sellerName: string
  sellerCredit: number
  coverSeed: number
  images: ItemImage[]
  history: HistoryEvent[]
  createdAt: string
  updatedAt?: string
  rejectReason?: string
  favorited?: boolean
}

export interface Order {
  id: string
  type: OrderType
  itemId: number
  itemTitle: string
  itemCoverSeed: number
  itemCondition: ItemCondition
  buyerId: number
  buyerName: string
  sellerId: number
  sellerName: string
  amount: number
  deliveryMethod: DeliveryMethod
  pickupPointId?: number
  pickupPointName?: string
  tradeStatus: TradeStatus
  afterSaleStatus: AfterSaleStatus
  createdAt: string
  payDeadline?: string
  confirmDeadline?: string
  paidAt?: string
  completedAt?: string
  reviewed?: boolean
  bountyId?: number
  planId?: number
  delivery?: DeliveryInfo
}

export interface DeliveryInfo {
  agreeTime?: string
  meetupPlace?: string
  verifyTime?: string
  inboundTime?: string
  pickupCode?: string
  outboundTime?: string
}

export interface Review {
  id: number
  orderId: string
  itemTitle: string
  reviewerId: number
  reviewerName: string
  targetName: string
  rating: number
  tags: string[]
  content: string
  status: 'valid' | 'void' | 'system'
  createdAt: string
}

export interface CreditRecord {
  id: number
  change: number
  reason: string
  orderId?: string
  createdAt: string
}

export interface PaymentRecord {
  id: number
  orderId?: string
  accountType: 'user' | 'platform'
  type: 'recharge' | 'payment' | 'payout' | 'refund' | 'chargeback'
  amount: number
  balanceAfter: number
  externalNo: string
  createdAt: string
  remark?: string
}

export interface Notification {
  id: number
  receiverId: number
  type: NotificationType
  title: string
  content: string
  read: boolean
  createdAt: string
  link?: string
}

export interface Bounty {
  id: number
  buyerId: number
  buyerName: string
  categoryId: number
  categoryName: string
  keyword: string
  budget: number
  condition: ItemCondition
  status: BountyStatus
  createdAt: string
  expiresAt: string
  matchCount: number
}

export interface ExchangeIntent {
  id: number
  userId: number
  userName: string
  myItemId: number
  myItemTitle: string
  myItemSeed: number
  matchType: 'specific' | 'condition'
  targetItemId?: number
  targetItemTitle?: string
  targetCategoryId?: number
  targetCategoryName?: string
  keyword?: string
  status: IntentStatus
  createdAt: string
}

export interface ExchangeParticipant {
  userId: number
  userName: string
  itemTitle: string
  itemSeed: number
  confirmed: boolean
}

export interface ExchangePlan {
  id: number
  ringType: 2 | 3
  participants: ExchangeParticipant[]
  status: PlanStatus
  deadline: string
  createdAt: string
}

export interface Arbitration {
  id: number
  orderId: string
  applicantName: string
  targetName: string
  type: string
  evidence: string
  status: ArbitrationStatus
  result?: 'buyer' | 'seller'
  handler?: string
  createdAt: string
  resolvedAt?: string
}

export interface AuditRecord {
  id: number
  itemId: number
  itemTitle: string
  result: 'approved' | 'rejected'
  reason?: string
  operator: string
  createdAt: string
}

export interface RosterEntry {
  studentNo: string
  name: string
  college: string
  major: string
  grade: string
}

export interface PublicStats {
  verifiedUsers: number
  onSaleItems: number
  completedOrders: number
  totalAmount: number
}

export interface ReportData {
  completedCount: number
  completedAmount: number
  refundRate: number
  pendingAudits: number
  pendingArbitrations: number
  categoryStats: { name: string; count: number; amount: number }[]
  monthlyStats: { month: string; count: number; amount: number }[]
}

export interface LoginResult {
  token: string
  user: User
}

export interface PageResult<T> {
  list: T[]
  total: number
}
