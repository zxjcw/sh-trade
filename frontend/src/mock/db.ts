import type {
  Arbitration,
  AuditRecord,
  Bounty,
  Category,
  CreditRecord,
  ExchangeIntent,
  ExchangePlan,
  Item,
  ItemCondition,
  ItemStatus,
  Notification,
  Order,
  PaymentRecord,
  PickupPoint,
  Review,
  RosterEntry,
  User
} from '@/types'

export interface MockDb {
  users: User[]
  passwords: Record<string, string>
  sessions: Record<string, number>
  categories: Category[]
  pickupPoints: PickupPoint[]
  items: Item[]
  orders: Order[]
  favorites: { userId: number; itemId: number }[]
  notifications: Notification[]
  paymentRecords: PaymentRecord[]
  creditRecords: CreditRecord[]
  reviews: Review[]
  bounties: Bounty[]
  intents: ExchangeIntent[]
  plans: ExchangePlan[]
  arbitrations: Arbitration[]
  auditRecords: AuditRecord[]
  roster: RosterEntry[]
}

const STORAGE_KEY = 'shiguangji_mock_db_v2'

const iso = (date: Date) => date.toISOString()
const daysAgo = (n: number, hour = 10) => {
  const d = new Date()
  d.setDate(d.getDate() - n)
  d.setHours(hour, 0, 0, 0)
  return iso(d)
}
const minutesFromNow = (n: number) => iso(new Date(Date.now() + n * 60_000))

function makeItem(input: {
  id: number
  title: string
  categoryId: number
  categoryName: string
  price: number
  originalPrice: number
  purchasePrice?: number
  condition: ItemCondition
  status: ItemStatus
  sellerId: number
  sellerName: string
  sellerCredit: number
  coverSeed: number
  description: string
  createdAt: string
  notesTag?: Item['notesTag']
  source?: Item['source']
  rejectReason?: string
  dealWith?: string
  dealTime?: string
}): Item {
  const history: Item['history'] = [
    {
      id: input.id * 10 + 1,
      type: 'created',
      time: input.createdAt,
      description: '物品建档，等待第一位主人'
    }
  ]
  if (input.status === 'completed' && input.dealWith) {
    history.push({
      id: input.id * 10 + 2,
      type: 'deal',
      time: input.dealTime ?? input.createdAt,
      description: `成交，新主人：${input.dealWith}`
    })
  }
  return {
    id: input.id,
    title: input.title,
    categoryId: input.categoryId,
    categoryName: input.categoryName,
    price: input.price,
    originalPrice: input.originalPrice,
    purchasePrice: input.purchasePrice ?? input.originalPrice,
    condition: input.condition,
    description: input.description,
    status: input.status,
    source: input.source ?? 'offline',
    notesTag: input.notesTag ?? 'no_notes',
    sellerId: input.sellerId,
    sellerName: input.sellerName,
    sellerCredit: input.sellerCredit,
    coverSeed: input.coverSeed,
    images: [
      { id: input.id * 10 + 1, seed: input.coverSeed, isCover: true, status: 'active' },
      { id: input.id * 10 + 2, seed: input.coverSeed + 2, isCover: false, status: 'active' },
      { id: input.id * 10 + 3, seed: input.coverSeed + 4, isCover: false, status: 'active' }
    ],
    history,
    createdAt: input.createdAt,
    updatedAt: input.createdAt,
    rejectReason: input.rejectReason
  }
}

function seed(): MockDb {
  const users: User[] = [
    {
      id: 1,
      username: 'student',
      nickname: '林小满',
      role: 'student',
      verifyStatus: 'verified',
      creditScore: 102,
      balance: 360,
      studentNo: '20230001',
      college: '计算机学院',
      major: '软件工程',
      grade: '2023',
      contact: '13800000001',
      createdAt: daysAgo(300)
    },
    {
      id: 2,
      username: 'seller',
      nickname: '陈屿',
      role: 'student',
      verifyStatus: 'verified',
      creditScore: 98,
      balance: 56,
      studentNo: '20220018',
      college: '机械工程学院',
      major: '机械设计制造',
      grade: '2022',
      contact: '13800000002',
      createdAt: daysAgo(420)
    },
    {
      id: 3,
      username: 'suli',
      nickname: '苏梨',
      role: 'student',
      verifyStatus: 'verified',
      creditScore: 105,
      balance: 120,
      studentNo: '20220106',
      college: '外国语学院',
      major: '英语',
      grade: '2022',
      contact: '13800000003',
      createdAt: daysAgo(380)
    },
    {
      id: 4,
      username: 'zhouye',
      nickname: '周野',
      role: 'student',
      verifyStatus: 'verified',
      creditScore: 95,
      balance: 32,
      studentNo: '20230311',
      college: '体育学院',
      major: '运动训练',
      grade: '2023',
      contact: '13800000004',
      createdAt: daysAgo(260)
    },
    {
      id: 5,
      username: 'guyan',
      nickname: '顾言',
      role: 'student',
      verifyStatus: 'verified',
      creditScore: 100,
      balance: 80,
      studentNo: '20210422',
      college: '艺术学院',
      major: '音乐表演',
      grade: '2021',
      contact: '13800000005',
      createdAt: daysAgo(600)
    },
    {
      id: 10,
      username: 'operator',
      nickname: '运营小周',
      role: 'operator',
      verifyStatus: 'verified',
      creditScore: 100,
      balance: 0,
      contact: '运营值班号',
      createdAt: daysAgo(500)
    },
    {
      id: 99,
      username: 'admin',
      nickname: '校方管理员',
      role: 'admin',
      verifyStatus: 'verified',
      creditScore: 100,
      balance: 0,
      contact: '学生事务中心',
      createdAt: daysAgo(700)
    }
  ]

  const categories: Category[] = [
    { id: 1, name: '教材书籍', parentId: null, enabled: true },
    { id: 11, name: '考研专区', parentId: 1, enabled: true },
    { id: 12, name: '专业教材', parentId: 1, enabled: true },
    { id: 13, name: '课外读物', parentId: 1, enabled: true },
    { id: 2, name: '数码电子', parentId: null, enabled: true },
    { id: 21, name: '手机平板', parentId: 2, enabled: true },
    { id: 22, name: '电脑配件', parentId: 2, enabled: true },
    { id: 23, name: '影音设备', parentId: 2, enabled: true },
    { id: 3, name: '生活用品', parentId: null, enabled: true },
    { id: 31, name: '宿舍电器', parentId: 3, enabled: true },
    { id: 32, name: '日用杂物', parentId: 3, enabled: true },
    { id: 4, name: '运动户外', parentId: null, enabled: true },
    { id: 41, name: '球类器材', parentId: 4, enabled: true },
    { id: 42, name: '骑行装备', parentId: 4, enabled: true },
    { id: 5, name: '乐器', parentId: null, enabled: true },
    { id: 51, name: '吉他 / 键盘', parentId: 5, enabled: true },
    { id: 52, name: '其他乐器', parentId: 5, enabled: true },
    { id: 6, name: '美妆个护', parentId: null, enabled: true },
    { id: 61, name: '护肤', parentId: 6, enabled: true },
    { id: 62, name: '彩妆', parentId: 6, enabled: true }
  ]

  const pickupPoints: PickupPoint[] = [
    { id: 1, name: '菜鸟驿站（一食堂店）', address: '一食堂西门旁 12 号' },
    { id: 2, name: '图书馆东门驿站', address: '图书馆东门一层' },
    { id: 3, name: '南区宿舍服务站', address: '南区 7 号楼架空层' }
  ]

  const items: Item[] = [
    makeItem({
      id: 1,
      title: '《高等数学》同济第七版 上下册',
      categoryId: 12,
      categoryName: '专业教材',
      price: 25,
      originalPrice: 89,
      purchasePrice: 79,
      condition: 'like_new',
      status: 'on_sale',
      sellerId: 2,
      sellerName: '陈屿',
      sellerCredit: 98,
      coverSeed: 1,
      notesTag: 'with_notes',
      description: '大一高数教材，书角有少量笔记，无缺页。适合提前预习或补考复习。',
      createdAt: daysAgo(6)
    }),
    makeItem({
      id: 2,
      title: 'iPad Air 5 64G 蓝色',
      categoryId: 21,
      categoryName: '手机平板',
      price: 2600,
      originalPrice: 4399,
      purchasePrice: 4299,
      condition: 'like_new',
      status: 'on_sale',
      sellerId: 3,
      sellerName: '苏梨',
      sellerCredit: 105,
      coverSeed: 2,
      description: '去年双十一入手，主要用来记笔记，屏幕无划痕，电池健康 96%。配原装充电器和保护壳。',
      createdAt: daysAgo(3)
    }),
    makeItem({
      id: 3,
      title: '宿舍小冰箱 50L',
      categoryId: 31,
      categoryName: '宿舍电器',
      price: 180,
      originalPrice: 499,
      purchasePrice: 459,
      condition: 'good',
      status: 'locked',
      sellerId: 4,
      sellerName: '周野',
      sellerCredit: 95,
      coverSeed: 3,
      description: '毕业出一台小冰箱，制冷正常，声音很小。仅限校内自提，可送到楼下。',
      createdAt: daysAgo(9)
    }),
    makeItem({
      id: 4,
      title: '捷安特公路自行车 M 码',
      categoryId: 42,
      categoryName: '骑行装备',
      price: 650,
      originalPrice: 1899,
      purchasePrice: 1699,
      condition: 'good',
      status: 'on_sale',
      sellerId: 2,
      sellerName: '陈屿',
      sellerCredit: 98,
      coverSeed: 4,
      description: '骑了两年，正常使用痕迹，刚换过刹车皮。适合通勤和周末骑行，面交可试骑。',
      createdAt: daysAgo(12)
    }),
    makeItem({
      id: 5,
      title: '雅马哈 F310 民谣吉他',
      categoryId: 51,
      categoryName: '吉他 / 键盘',
      price: 450,
      originalPrice: 1200,
      purchasePrice: 999,
      condition: 'like_new',
      status: 'locked',
      sellerId: 5,
      sellerName: '顾言',
      sellerCredit: 100,
      coverSeed: 5,
      description: '入门神器，音色干净，送琴包、变调夹和备用琴弦。因换电箱琴出。',
      createdAt: daysAgo(8)
    }),
    makeItem({
      id: 6,
      title: '考研英语真题 + 高频单词书',
      categoryId: 11,
      categoryName: '考研专区',
      price: 20,
      originalPrice: 120,
      purchasePrice: 108,
      condition: 'new',
      status: 'on_sale',
      sellerId: 3,
      sellerName: '苏梨',
      sellerCredit: 105,
      coverSeed: 6,
      notesTag: 'with_answers',
      description: '买多了，一套全新未拆。真题 2010–2024，单词书按考频排序。',
      createdAt: daysAgo(2)
    }),
    makeItem({
      id: 7,
      title: '机械键盘 87 键 茶轴',
      categoryId: 22,
      categoryName: '电脑配件',
      price: 160,
      originalPrice: 399,
      purchasePrice: 359,
      condition: 'like_new',
      status: 'locked',
      sellerId: 2,
      sellerName: '陈屿',
      sellerCredit: 98,
      coverSeed: 7,
      description: '办公室用了一学期，键帽无打油，支持三模连接，附赠拔键器。',
      createdAt: daysAgo(4)
    }),
    makeItem({
      id: 8,
      title: '护眼台灯 国AA级',
      categoryId: 31,
      categoryName: '宿舍电器',
      price: 45,
      originalPrice: 129,
      purchasePrice: 119,
      condition: 'good',
      status: 'on_sale',
      sellerId: 4,
      sellerName: '周野',
      sellerCredit: 95,
      coverSeed: 8,
      description: '三档色温，无频闪，宿舍书桌刚好。搬家带不走，低价出。',
      createdAt: daysAgo(5)
    }),
    makeItem({
      id: 9,
      title: '军训迷彩服 175 码',
      categoryId: 32,
      categoryName: '日用杂物',
      price: 30,
      originalPrice: 80,
      purchasePrice: 80,
      condition: 'good',
      status: 'pending',
      sellerId: 1,
      sellerName: '林小满',
      sellerCredit: 102,
      coverSeed: 9,
      description: '军训结束后洗干净收好，175 码，含帽子和腰带。',
      createdAt: daysAgo(1)
    }),
    makeItem({
      id: 10,
      title: '蓝牙音箱 便携防水',
      categoryId: 23,
      categoryName: '影音设备',
      price: 120,
      originalPrice: 299,
      purchasePrice: 269,
      condition: 'like_new',
      status: 'locked',
      sellerId: 3,
      sellerName: '苏梨',
      sellerCredit: 105,
      coverSeed: 10,
      description: '音质不错，续航约 10 小时，防水等级 IPX7。买来只露营用过两次。',
      createdAt: daysAgo(7)
    }),
    makeItem({
      id: 11,
      title: '人体工学电竞椅 灰色',
      categoryId: 32,
      categoryName: '日用杂物',
      price: 320,
      originalPrice: 899,
      purchasePrice: 799,
      condition: 'good',
      status: 'completed',
      sellerId: 1,
      sellerName: '林小满',
      sellerCredit: 102,
      coverSeed: 11,
      description: '升降和靠背调节正常，扶手有轻微磨损。宿舍换布局出。',
      createdAt: daysAgo(20),
      dealWith: '林小满',
      dealTime: minutesFromNow(-3)
    }),
    makeItem({
      id: 12,
      title: '机械制图工具套装',
      categoryId: 12,
      categoryName: '专业教材',
      price: 40,
      originalPrice: 120,
      purchasePrice: 108,
      condition: 'good',
      status: 'locked',
      sellerId: 1,
      sellerName: '林小满',
      sellerCredit: 102,
      coverSeed: 12,
      description: '圆规、分规、三角板、丁字尺齐全，工图课用过一学期。',
      createdAt: daysAgo(10)
    }),
    makeItem({
      id: 13,
      title: '旧教材一套（高数 + 线代）',
      categoryId: 12,
      categoryName: '专业教材',
      price: 15,
      originalPrice: 96,
      purchasePrice: 90,
      condition: 'fair',
      status: 'pending',
      sellerId: 1,
      sellerName: '林小满',
      sellerCredit: 102,
      coverSeed: 13,
      notesTag: 'with_notes',
      description: '书页有笔记和荧光笔标记，不影响阅读。适合预算有限的同学。',
      createdAt: daysAgo(2)
    }),
    makeItem({
      id: 14,
      title: '羽毛球拍 碳素对拍',
      categoryId: 41,
      categoryName: '球类器材',
      price: 90,
      originalPrice: 260,
      purchasePrice: 240,
      condition: 'like_new',
      status: 'pending',
      sellerId: 2,
      sellerName: '陈屿',
      sellerCredit: 98,
      coverSeed: 14,
      description: '打了几次，拍框无磕碰，送两个拍套和三个球。',
      createdAt: daysAgo(1)
    }),
    makeItem({
      id: 15,
      title: '27 寸 2K 显示器',
      categoryId: 22,
      categoryName: '电脑配件',
      price: 800,
      originalPrice: 1599,
      purchasePrice: 1499,
      condition: 'good',
      status: 'on_sale',
      sellerId: 5,
      sellerName: '顾言',
      sellerCredit: 100,
      coverSeed: 15,
      description: 'IPS 屏，无坏点，支持 75Hz。因换了双屏出，附 HDMI 线。',
      createdAt: daysAgo(4)
    }),
    makeItem({
      id: 16,
      title: '斯伯丁篮球 7 号',
      categoryId: 41,
      categoryName: '球类器材',
      price: 60,
      originalPrice: 150,
      purchasePrice: 139,
      condition: 'fair',
      status: 'on_sale',
      sellerId: 4,
      sellerName: '周野',
      sellerCredit: 95,
      coverSeed: 16,
      description: '室外场用过一学期，手感还在，气足。毕业清仓。',
      createdAt: daysAgo(11)
    }),
    makeItem({
      id: 17,
      title: '桌面收纳盒三件套',
      categoryId: 32,
      categoryName: '日用杂物',
      price: 18,
      originalPrice: 59,
      purchasePrice: 55,
      condition: 'like_new',
      status: 'on_sale',
      sellerId: 5,
      sellerName: '顾言',
      sellerCredit: 100,
      coverSeed: 17,
      description: '白色磨砂质感，尺寸适合宿舍书桌，三件一起出。',
      createdAt: daysAgo(6)
    }),
    makeItem({
      id: 18,
      title: '考研政治全套（精讲精练 + 1000 题）',
      categoryId: 11,
      categoryName: '考研专区',
      price: 20,
      originalPrice: 110,
      purchasePrice: 100,
      condition: 'good',
      status: 'completed',
      sellerId: 1,
      sellerName: '林小满',
      sellerCredit: 102,
      coverSeed: 18,
      notesTag: 'with_notes',
      description: '上岸后出，书内有少量重点标记，附赠思维导图。',
      createdAt: daysAgo(40),
      dealWith: '林小满',
      dealTime: daysAgo(7)
    }),
    makeItem({
      id: 19,
      title: '宿舍桌面收纳盒',
      categoryId: 32,
      categoryName: '日用杂物',
      price: 50,
      originalPrice: 99,
      purchasePrice: 89,
      condition: 'good',
      status: 'completed',
      sellerId: 5,
      sellerName: '顾言',
      sellerCredit: 100,
      coverSeed: 19,
      description: '两层抽屉，放文具和护肤品刚好。',
      createdAt: daysAgo(30),
      dealWith: '顾言',
      dealTime: daysAgo(5)
    })
  ]

  const orders: Order[] = [
    {
      id: 'SO20261117001',
      type: 'normal',
      itemId: 7,
      itemTitle: '机械键盘 87 键 茶轴',
      itemCoverSeed: 7,
      itemCondition: 'like_new',
      buyerId: 1,
      buyerName: '林小满',
      sellerId: 2,
      sellerName: '陈屿',
      amount: 160,
      deliveryMethod: 'pickup',
      pickupPointId: 1,
      pickupPointName: '菜鸟驿站（一食堂店）',
      tradeStatus: 'pending_payment',
      afterSaleStatus: 'none',
      createdAt: daysAgo(0, 9),
      payDeadline: minutesFromNow(10)
    },
    {
      id: 'SO20261116002',
      type: 'normal',
      itemId: 10,
      itemTitle: '蓝牙音箱 便携防水',
      itemCoverSeed: 10,
      itemCondition: 'like_new',
      buyerId: 1,
      buyerName: '林小满',
      sellerId: 3,
      sellerName: '苏梨',
      amount: 120,
      deliveryMethod: 'pickup',
      pickupPointId: 2,
      pickupPointName: '图书馆东门驿站',
      tradeStatus: 'pending_delivery',
      afterSaleStatus: 'arbitrating',
      createdAt: daysAgo(1),
      paidAt: daysAgo(1)
    },
    {
      id: 'SO20261116003',
      type: 'normal',
      itemId: 3,
      itemTitle: '宿舍小冰箱 50L',
      itemCoverSeed: 3,
      itemCondition: 'good',
      buyerId: 1,
      buyerName: '林小满',
      sellerId: 4,
      sellerName: '周野',
      amount: 180,
      deliveryMethod: 'meetup',
      tradeStatus: 'pending_confirm',
      afterSaleStatus: 'none',
      createdAt: daysAgo(1, 14),
      paidAt: daysAgo(1, 14),
      confirmDeadline: minutesFromNow(8),
      delivery: {
        agreeTime: daysAgo(1, 16),
        meetupPlace: '一食堂门口',
        verifyTime: minutesFromNow(-30)
      }
    },
    {
      id: 'SO20261115004',
      type: 'normal',
      itemId: 11,
      itemTitle: '人体工学电竞椅 灰色',
      itemCoverSeed: 11,
      itemCondition: 'good',
      buyerId: 1,
      buyerName: '林小满',
      sellerId: 2,
      sellerName: '陈屿',
      amount: 320,
      deliveryMethod: 'meetup',
      tradeStatus: 'completed',
      afterSaleStatus: 'none',
      createdAt: minutesFromNow(-40),
      paidAt: minutesFromNow(-35),
      completedAt: minutesFromNow(-3),
      reviewed: false,
      delivery: {
        agreeTime: minutesFromNow(-30),
        meetupPlace: '北区宿舍 3 栋楼下',
        verifyTime: minutesFromNow(-20)
      }
    },
    {
      id: 'SO20261110005',
      type: 'normal',
      itemId: 18,
      itemTitle: '考研政治全套（精讲精练 + 1000 题）',
      itemCoverSeed: 18,
      itemCondition: 'good',
      buyerId: 1,
      buyerName: '林小满',
      sellerId: 3,
      sellerName: '苏梨',
      amount: 20,
      deliveryMethod: 'pickup',
      pickupPointId: 3,
      pickupPointName: '南区宿舍服务站',
      tradeStatus: 'completed',
      afterSaleStatus: 'none',
      createdAt: daysAgo(9),
      paidAt: daysAgo(9),
      completedAt: daysAgo(7),
      reviewed: true
    },
    {
      id: 'SO20261112006',
      type: 'normal',
      itemId: 19,
      itemTitle: '宿舍桌面收纳盒',
      itemCoverSeed: 19,
      itemCondition: 'good',
      buyerId: 5,
      buyerName: '顾言',
      sellerId: 1,
      sellerName: '林小满',
      amount: 50,
      deliveryMethod: 'meetup',
      tradeStatus: 'completed',
      afterSaleStatus: 'none',
      createdAt: daysAgo(6),
      paidAt: daysAgo(6),
      completedAt: daysAgo(5),
      reviewed: true
    }
  ]

  const notifications: Notification[] = [
    {
      id: 1,
      receiverId: 1,
      type: 'order_completed',
      title: '订单已完成',
      content: '「人体工学电竞椅 灰色」已确认收货，320 元已放款给卖家，双方信用 +2。',
      read: false,
      createdAt: minutesFromNow(-3),
      link: '/orders/SO20261115004'
    },
    {
      id: 2,
      receiverId: 1,
      type: 'bounty_match',
      title: '悬赏命中',
      content: '卖家「陈屿」上架的「机械键盘 87 键 茶轴」符合你的求购条件，去看看？',
      read: false,
      createdAt: daysAgo(2),
      link: '/items/7'
    },
    {
      id: 3,
      receiverId: 1,
      type: 'arbitration_accepted',
      title: '仲裁已受理',
      content: '订单 SO20261116002 的售后争议已转运营仲裁，处理结果会通过站内信通知。',
      read: false,
      createdAt: daysAgo(1),
      link: '/orders/SO20261116002'
    },
    {
      id: 4,
      receiverId: 1,
      type: 'audit',
      title: '物品审核通过',
      content: '你发布的「宿舍桌面收纳盒」已通过审核并上架。',
      read: true,
      createdAt: daysAgo(28),
      link: '/items/19'
    },
    {
      id: 5,
      receiverId: 1,
      type: 'verification',
      title: '学籍认证通过',
      content: '欢迎加入拾光集，你已获得发布、下单、求购与置换资格。',
      read: true,
      createdAt: daysAgo(299)
    }
  ]

  const paymentRecords: PaymentRecord[] = [
    {
      id: 1,
      accountType: 'user',
      type: 'recharge',
      amount: 1000,
      balanceAfter: 1000,
      externalNo: 'RC20261110001',
      createdAt: daysAgo(8),
      remark: '钱包充值'
    },
    {
      id: 2,
      orderId: 'SO20261116002',
      accountType: 'user',
      type: 'payment',
      amount: 120,
      balanceAfter: 880,
      externalNo: 'PAY20261116002',
      createdAt: daysAgo(1),
      remark: '订单支付'
    },
    {
      id: 3,
      orderId: 'SO20261116003',
      accountType: 'user',
      type: 'payment',
      amount: 180,
      balanceAfter: 700,
      externalNo: 'PAY20261116003',
      createdAt: daysAgo(1),
      remark: '订单支付'
    },
    {
      id: 4,
      orderId: 'SO20261115004',
      accountType: 'user',
      type: 'payment',
      amount: 320,
      balanceAfter: 380,
      externalNo: 'PAY20261115004',
      createdAt: daysAgo(3),
      remark: '订单支付'
    },
    {
      id: 5,
      orderId: 'SO20261110005',
      accountType: 'user',
      type: 'payment',
      amount: 20,
      balanceAfter: 360,
      externalNo: 'PAY20261110005',
      createdAt: daysAgo(9),
      remark: '订单支付'
    }
  ]

  const creditRecords: CreditRecord[] = [
    { id: 1, change: 2, reason: '完成交易', orderId: 'SO20261115004', createdAt: minutesFromNow(-3) },
    { id: 2, change: 2, reason: '完成交易', orderId: 'SO20261110005', createdAt: daysAgo(7) },
    { id: 3, change: 2, reason: '完成交易', orderId: 'SO20261112006', createdAt: daysAgo(5) }
  ]

  const reviews: Review[] = [
    {
      id: 1,
      orderId: 'SO20261110005',
      itemTitle: '考研政治全套（精讲精练 + 1000 题）',
      reviewerId: 1,
      reviewerName: '林小满',
      targetName: '苏梨',
      rating: 5,
      tags: ['描述相符', '发货快', '有笔记'],
      content: '书保存得很好，重点标记很清晰，卖家还送了思维导图。',
      status: 'valid',
      createdAt: daysAgo(7)
    },
    {
      id: 2,
      orderId: 'SO20261112006',
      itemTitle: '宿舍桌面收纳盒',
      reviewerId: 5,
      reviewerName: '顾言',
      targetName: '林小满',
      rating: 5,
      tags: ['沟通顺畅', '成色好'],
      content: '收纳盒很干净，面交准时。',
      status: 'valid',
      createdAt: daysAgo(5)
    }
  ]

  const bounties: Bounty[] = [
    {
      id: 1,
      buyerId: 3,
      buyerName: '苏梨',
      categoryId: 11,
      categoryName: '考研专区',
      keyword: '线性代数 教材',
      budget: 30,
      condition: 'good',
      status: 'open',
      createdAt: daysAgo(2),
      expiresAt: minutesFromNow(60 * 24 * 5),
      matchCount: 0
    },
    {
      id: 2,
      buyerId: 4,
      buyerName: '周野',
      categoryId: 22,
      categoryName: '电脑配件',
      keyword: '27 寸 显示器',
      budget: 800,
      condition: 'good',
      status: 'open',
      createdAt: daysAgo(1),
      expiresAt: minutesFromNow(60 * 24 * 4),
      matchCount: 1
    },
    {
      id: 3,
      buyerId: 5,
      buyerName: '顾言',
      categoryId: 41,
      categoryName: '球类器材',
      keyword: '羽毛球拍',
      budget: 150,
      condition: 'like_new',
      status: 'open',
      createdAt: daysAgo(3),
      expiresAt: minutesFromNow(60 * 24 * 7),
      matchCount: 0
    }
  ]

  const intents: ExchangeIntent[] = [
    {
      id: 1,
      userId: 1,
      userName: '林小满',
      myItemId: 12,
      myItemTitle: '机械制图工具套装',
      myItemSeed: 12,
      matchType: 'specific',
      targetItemId: 5,
      targetItemTitle: '雅马哈 F310 民谣吉他',
      status: 'active',
      createdAt: daysAgo(1)
    },
    {
      id: 2,
      userId: 5,
      userName: '顾言',
      myItemId: 5,
      myItemTitle: '雅马哈 F310 民谣吉他',
      myItemSeed: 5,
      matchType: 'specific',
      targetItemId: 12,
      targetItemTitle: '机械制图工具套装',
      status: 'active',
      createdAt: daysAgo(1)
    }
  ]

  const plans: ExchangePlan[] = [
    {
      id: 1,
      ringType: 2,
      participants: [
        { userId: 1, userName: '林小满', itemTitle: '机械制图工具套装', itemSeed: 12, confirmed: false },
        { userId: 5, userName: '顾言', itemTitle: '雅马哈 F310 民谣吉他', itemSeed: 5, confirmed: true }
      ],
      status: 'pending',
      deadline: minutesFromNow(10),
      createdAt: minutesFromNow(-20)
    }
  ]

  const arbitrations: Arbitration[] = [
    {
      id: 1,
      orderId: 'SO20261116002',
      applicantName: '林小满',
      targetName: '苏梨',
      type: '描述不符',
      evidence: '音箱外壳有明显磕碰，发布页写的是九成新且未提磕碰，和卖家沟通后被拒绝处理。',
      status: 'pending',
      createdAt: daysAgo(0, 8)
    }
  ]

  const auditRecords: AuditRecord[] = [
    {
      id: 1,
      itemId: 11,
      itemTitle: '人体工学电竞椅 灰色',
      result: 'approved',
      operator: '运营小周',
      createdAt: daysAgo(20)
    },
    {
      id: 2,
      itemId: 19,
      itemTitle: '宿舍桌面收纳盒',
      result: 'approved',
      operator: '运营小周',
      createdAt: daysAgo(29)
    }
  ]

  const roster: RosterEntry[] = [
    { studentNo: '20230001', name: '林小满', college: '计算机学院', major: '软件工程', grade: '2023' },
    { studentNo: '20220018', name: '陈屿', college: '机械工程学院', major: '机械设计制造', grade: '2022' },
    { studentNo: '20220106', name: '苏梨', college: '外国语学院', major: '英语', grade: '2022' },
    { studentNo: '20230311', name: '周野', college: '体育学院', major: '运动训练', grade: '2023' },
    { studentNo: '20210422', name: '顾言', college: '艺术学院', major: '音乐表演', grade: '2021' },
    { studentNo: '20240007', name: '沈知夏', college: '经济管理学院', major: '工商管理', grade: '2024' }
  ]

  return {
    users,
    passwords: {
      student: '123456',
      seller: '123456',
      suli: '123456',
      zhouye: '123456',
      guyan: '123456',
      operator: '123456',
      admin: '123456'
    },
    sessions: {},
    categories,
    pickupPoints,
    items,
    orders,
    favorites: [
      { userId: 1, itemId: 2 },
      { userId: 1, itemId: 4 }
    ],
    notifications,
    paymentRecords,
    creditRecords,
    reviews,
    bounties,
    intents,
    plans,
    arbitrations,
    auditRecords,
    roster
  }
}

function load(): MockDb {
  if (typeof localStorage === 'undefined') return seed()
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw) as MockDb
  } catch {
    // 忽略损坏的本地缓存，重新生成种子数据
  }
  const data = seed()
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  return data
}

let db: MockDb = load()

export function getDb(): MockDb {
  return db
}

export function saveDb(): void {
  if (typeof localStorage === 'undefined') return
  localStorage.setItem(STORAGE_KEY, JSON.stringify(db))
}

export function resetDb(): void {
  db = seed()
  saveDb()
}

export function nextId(list: { id: number }[]): number {
  return list.reduce((max, item) => Math.max(max, item.id), 0) + 1
}
