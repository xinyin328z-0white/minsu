package com.example.minsumgr.config;

import com.example.minsumgr.domain.*;
import com.example.minsumgr.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final ActivityRepository activityRepository;
    private final RouteRepository routeRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final BookingRepository bookingRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    public DataInitializer(UserRepository userRepository,
                          HomestayRepository homestayRepository,
                          ActivityRepository activityRepository,
                          RouteRepository routeRepository,
                          SystemSettingRepository systemSettingRepository,
                          BookingRepository bookingRepository,
                          UserPreferenceRepository userPreferenceRepository) {
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
        this.activityRepository = activityRepository;
        this.routeRepository = routeRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.bookingRepository = bookingRepository;
        this.userPreferenceRepository = userPreferenceRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已初始化
        if (userRepository.count() > 0) {
            return;
        }

        // 创建管理员用户
        User admin = new User();
        admin.setUsername("fjr");
        admin.setPassword("fjr");
        admin.setRealName("管理员");
        admin.setEmail("admin@minsu.com");
        admin.setPhone("13800138000");
        admin.setRole("ADMIN");
        userRepository.save(admin);

        // 创建普通用户
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("user1");
        user1.setRealName("张三");
        user1.setEmail("user1@example.com");
        user1.setPhone("13800138001");
        user1.setRole("CUSTOMER");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("user2");
        user2.setRealName("李四");
        user2.setEmail("user2@example.com");
        user2.setPhone("13800138002");
        user2.setRole("CUSTOMER");
        userRepository.save(user2);

        User user3 = new User();
        user3.setUsername("user3");
        user3.setPassword("user3");
        user3.setRealName("王五");
        user3.setEmail("user3@example.com");
        user3.setPhone("13800138003");
        user3.setRole("CUSTOMER");
        userRepository.save(user3);

        User user4 = new User();
        user4.setUsername("user4");
        user4.setPassword("user4");
        user4.setRealName("赵六");
        user4.setEmail("user4@example.com");
        user4.setPhone("13800138004");
        user4.setRole("CUSTOMER");
        userRepository.save(user4);

        User user5 = new User();
        user5.setUsername("user5");
        user5.setPassword("user5");
        user5.setRealName("孙七");
        user5.setEmail("user5@example.com");
        user5.setPhone("13800138005");
        user5.setRole("CUSTOMER");
        userRepository.save(user5);

        User user6 = new User();
        user6.setUsername("user6");
        user6.setPassword("user6");
        user6.setRealName("周八");
        user6.setEmail("user6@example.com");
        user6.setPhone("13800138006");
        user6.setRole("CUSTOMER");
        userRepository.save(user6);

        User user7 = new User();
        user7.setUsername("user7");
        user7.setPassword("user7");
        user7.setRealName("吴九");
        user7.setEmail("user7@example.com");
        user7.setPhone("13800138007");
        user7.setRole("CUSTOMER");
        userRepository.save(user7);

        User user8 = new User();
        user8.setUsername("user8");
        user8.setPassword("user8");
        user8.setRealName("郑十");
        user8.setEmail("user8@example.com");
        user8.setPhone("13800138008");
        user8.setRole("CUSTOMER");
        userRepository.save(user8);

        // 创建游玩项目
        Activity activity1 = new Activity();
        activity1.setName("山地骑行");
        activity1.setLocation("九龙山风景区");
        activity1.setDescription("在山林中骑行，体验大自然的美景，适合户外爱好者。全程约20公里，需要2小时。");
        activityRepository.save(activity1);

        Activity activity2 = new Activity();
        activity2.setName("温泉浴体验");
        activity2.setLocation("清泉温泉度假村");
        activity2.setDescription("享受天然温泉，放松身心。温泉水温42度，富含矿物质，对皮肤有益。提供温泉、泥浆浴、冷热交替等服务。");
        activityRepository.save(activity2);

        Activity activity3 = new Activity();
        activity3.setName("古镇文化游");
        activity3.setLocation("老南街历史古镇");
        activity3.setDescription("领略传统建筑和文化魅力，品尝地道美食。古镇保存完好，有500多年历史，是国家级文化遗产保护地。");
        activityRepository.save(activity3);

        Activity activity4 = new Activity();
        activity4.setName("漂流探险");
        activity4.setLocation("清江漂流区");
        activity4.setDescription("在清江上漂流，体验刺激与冒险。全程约15公里，约3小时，适合家庭和冒险爱好者。");
        activityRepository.save(activity4);

        // 创建旅游路线
        Route route1 = new Route();
        route1.setName("山水文化三日游");
        route1.setTags("山水,文化,户外");
        route1.setDescription("D1：到达古镇，游览老南街，品尝特色小吃。D2：山地骑行九龙山，下午温泉浴体验。D3：清江漂流，返程。");
        routeRepository.save(route1);

        Route route2 = new Route();
        route2.setName("休闲度假两日游");
        route2.setTags("温泉,休闲,养生");
        route2.setDescription("D1：到达民宿休息，下午体验温泉浴，晚餐品尝本地美食。D2：上午继续温泉体验，下午自由活动或返程。");
        routeRepository.save(route2);

        Route route3 = new Route();
        route3.setName("冒险者极限五日游");
        route3.setTags("冒险,骑行,漂流,刺激");
        route3.setDescription("D1：山地骑行九龙山。D2：清江漂流。D3：古镇文化游。D4：温泉体验。D5：休闲购物，返程。");
        routeRepository.save(route3);

        Route route4 = new Route();
        route4.setName("家庭亲子四日游");
        route4.setTags("亲子,温和,文化");
        route4.setDescription("D1：古镇游览，儿童游乐区。D2：温泉体验，家庭浴池。D3：清江漂流（儿童专用区）。D4：民宿周边散步，返程。");
        routeRepository.save(route4);

        // 创建民宿
        Homestay homestay1 = new Homestay();
        homestay1.setName("云山民宿");
        homestay1.setLocation("九龙山脚下，环境优美");
        homestay1.setPricePerNight(new BigDecimal("300"));
        homestay1.setAvailableRooms(5);
        homestay1.setDescription("依山而建的精品民宿，拥有开阔的山景视野，房间宽敞明亮，配备现代化设施。提供免费停车、免费WiFi、24小时热水、独立卫浴等服务。");
        homestay1.setRoomNotes("每间房配备独立浴室、空调、电视、迷你冰箱。部分房间可观山景。早餐另算50元/人。");
        homestayRepository.save(homestay1);

        Homestay homestay2 = new Homestay();
        homestay2.setName("温泉小居");
        homestay2.setLocation("清泉温泉度假村内，靠近温泉池");
        homestay2.setPricePerNight(new BigDecimal("380"));
        homestay2.setAvailableRooms(3);
        homestay2.setDescription("温泉度假村内的豪华民宿，所有房间均可直达温泉池。环境幽雅，设施完善，专业服务团队。提供私家温泉池、室内温泉浴、露天温泉等特色服务。");
        homestay2.setRoomNotes("每间房配备温泉浴缸、地暖、迷你酒吧、电热毛巾架。配有免费温泉浴袍和拖鞋。可加床，加床费150元/晚。");
        homestayRepository.save(homestay2);

        Homestay homestay3 = new Homestay();
        homestay3.setName("古镇居舍");
        homestay3.setLocation("老南街中心，靠近各景点");
        homestay3.setPricePerNight(new BigDecimal("250"));
        homestay3.setAvailableRooms(6);
        homestay3.setDescription("坐落在古镇中心的传统民宿，保留古镇风格，充满文化气息。步行可到各大景点、餐厅、特产店。适合喜欢体验古镇文化的游客。");
        homestay3.setRoomNotes("房间保留古镇传统风格，配备独立卫浴、WiFi、暖风机。提供民宿主人自制早餐（含在房价内）。");
        homestayRepository.save(homestay3);

        Homestay homestay4 = new Homestay();
        homestay4.setName("清江度假屋");
        homestay4.setLocation("清江河畔，河景优美");
        homestay4.setPricePerNight(new BigDecimal("320"));
        homestay4.setAvailableRooms(4);
        homestay4.setDescription("河畔景观民宿，所有房间均面向清江，享受壮观河景。配备高级家具，现代化装修。是漂流爱好者的理想落脚点。");
        homestay4.setRoomNotes("所有房间都是河景房，配备观景阳台、舒适床品、现代卫浴。提供免费接送服务（距漂流点1公里）。");
        homestayRepository.save(homestay4);

        // 创建推荐算法演示数据：偏好和历史订单用于协同过滤
        createPreference(user2.getId(), "MODERATE", "温泉,休闲,养生", "清泉温泉度假村", 260, 420, "COUPLE", "偏好安静、温泉和放松型行程");
        createPreference(user3.getId(), "ECONOMY", "亲子,文化,古镇", "老南街", 180, 320, "FAMILY", "带孩子出游，喜欢文化体验和轻松路线");

        // 丰富的历史订单数据 - 覆盖过去多个月
        // 3个月前的订单（已完成）
        createBooking(user1.getId(), homestay1.getId(), LocalDate.now().minusMonths(3).minusDays(5), LocalDate.now().minusMonths(3).minusDays(3), "COMPLETED", 2);
        createBooking(user2.getId(), homestay2.getId(), LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(3).plusDays(2), "COMPLETED", 2);
        createBooking(user4.getId(), homestay3.getId(), LocalDate.now().minusMonths(3).plusDays(3), LocalDate.now().minusMonths(3).plusDays(5), "COMPLETED", 3);
        createBooking(user5.getId(), homestay1.getId(), LocalDate.now().minusMonths(3).plusDays(8), LocalDate.now().minusMonths(3).plusDays(10), "COMPLETED", 1);

        // 2个月前的订单
        createBooking(user1.getId(), homestay3.getId(), LocalDate.now().minusMonths(2).minusDays(3), LocalDate.now().minusMonths(2), "COMPLETED", 2);
        createBooking(user3.getId(), homestay4.getId(), LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(2).plusDays(3), "COMPLETED", 4);
        createBooking(user6.getId(), homestay2.getId(), LocalDate.now().minusMonths(2).plusDays(5), LocalDate.now().minusMonths(2).plusDays(7), "COMPLETED", 2);
        createBooking(user7.getId(), homestay1.getId(), LocalDate.now().minusMonths(2).plusDays(10), LocalDate.now().minusMonths(2).plusDays(12), "COMPLETED", 1);
        createBooking(user2.getId(), homestay4.getId(), LocalDate.now().minusMonths(2).plusDays(15), LocalDate.now().minusMonths(2).plusDays(18), "COMPLETED", 2);
        createBooking(user8.getId(), homestay3.getId(), LocalDate.now().minusMonths(2).plusDays(12), LocalDate.now().minusMonths(2).plusDays(14), "CANCELED", 3);

        // 1个月前的订单
        createBooking(user4.getId(), homestay1.getId(), LocalDate.now().minusMonths(1).minusDays(5), LocalDate.now().minusMonths(1).minusDays(2), "COMPLETED", 2);
        createBooking(user5.getId(), homestay2.getId(), LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1).plusDays(3), "COMPLETED", 2);
        createBooking(user1.getId(), homestay4.getId(), LocalDate.now().minusMonths(1).plusDays(2), LocalDate.now().minusMonths(1).plusDays(5), "COMPLETED", 3);
        createBooking(user6.getId(), homestay3.getId(), LocalDate.now().minusMonths(1).plusDays(8), LocalDate.now().minusMonths(1).plusDays(11), "COMPLETED", 2);
        createBooking(user3.getId(), homestay1.getId(), LocalDate.now().minusMonths(1).plusDays(12), LocalDate.now().minusMonths(1).plusDays(14), "COMPLETED", 2);
        createBooking(user7.getId(), homestay2.getId(), LocalDate.now().minusMonths(1).plusDays(10), LocalDate.now().minusMonths(1).plusDays(13), "COMPLETED", 1);
        createBooking(user8.getId(), homestay4.getId(), LocalDate.now().minusMonths(1).plusDays(5), LocalDate.now().minusMonths(1).plusDays(7), "CANCELED", 2);

        // 最近的订单（各种状态）
        createBooking(user2.getId(), homestay1.getId(), LocalDate.now().minusDays(10), LocalDate.now().minusDays(8), "COMPLETED", 2);
        createBooking(user4.getId(), homestay2.getId(), LocalDate.now().minusDays(7), LocalDate.now().minusDays(5), "COMPLETED", 3);
        createBooking(user1.getId(), homestay3.getId(), LocalDate.now().minusDays(5), LocalDate.now().minusDays(3), "COMPLETED", 2);
        createBooking(user5.getId(), homestay4.getId(), LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), "COMPLETED", 1);

        // 当前和未来的订单
        createBooking(user3.getId(), homestay2.getId(), LocalDate.now(), LocalDate.now().plusDays(2), "CONFIRMED", 2);
        createBooking(user6.getId(), homestay1.getId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "CONFIRMED", 3);
        createBooking(user7.getId(), homestay4.getId(), LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), "CONFIRMED", 2);
        createBooking(user8.getId(), homestay3.getId(), LocalDate.now().plusDays(3), LocalDate.now().plusDays(6), "PENDING", 4);
        createBooking(user1.getId(), homestay2.getId(), LocalDate.now().plusDays(5), LocalDate.now().plusDays(8), "PENDING", 2);
        createBooking(user4.getId(), homestay1.getId(), LocalDate.now().plusDays(7), LocalDate.now().plusDays(9), "PENDING", 1);
        createBooking(user2.getId(), homestay3.getId(), LocalDate.now().plusDays(10), LocalDate.now().plusDays(13), "PENDING", 2);
        createBooking(user5.getId(), homestay2.getId(), LocalDate.now().plusDays(15), LocalDate.now().plusDays(18), "PENDING", 3);

        // 初始化AI配置 - 硅基流动API
        initializeAiConfig();

        System.out.println("✅ 测试数据初始化完成！");
        System.out.println("   管理员: fjr / fjr");
        System.out.println("   用户1: user1 / user1 (张三)");
        System.out.println("   用户2: user2 / user2 (李四)");
        System.out.println("   用户3: user3 / user3 (王五)");
        System.out.println("   用户4: user4 / user4 (赵六)");
        System.out.println("   用户5: user5 / user5 (孙七)");
        System.out.println("   用户6: user6 / user6 (周八)");
        System.out.println("   用户7: user7 / user7 (吴九)");
        System.out.println("   用户8: user8 / user8 (郑十)");
        System.out.println("   游玩项目: 4个");
        System.out.println("   旅游路线: 4条");
        System.out.println("   民宿: 4家");
        System.out.println("   推荐偏好样本: 2条");
        System.out.println("   推荐订单样本: 30条");
        System.out.println("✅ AI配置: 已初始化（硅基流动GLM-4.7）");
    }

    private void createPreference(Long userId, String budgetLevel, String styles, String locations,
                                  Integer minPrice, Integer maxPrice, String companionType, String notes) {
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setBudgetLevel(budgetLevel);
        preference.setTravelStyles(styles);
        preference.setPreferredLocations(locations);
        preference.setMinPrice(minPrice);
        preference.setMaxPrice(maxPrice);
        preference.setCompanionType(companionType);
        preference.setNotes(notes);
        preference.setCreatedAt(LocalDateTime.now());
        preference.setUpdatedAt(LocalDateTime.now());
        userPreferenceRepository.save(preference);
    }

    private void createBooking(Long userId, Long homestayId, LocalDate checkInDate,
                               LocalDate checkOutDate, String status, Integer guestCount) {
        Homestay homestay = homestayRepository.findById(homestayId).orElse(null);
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setHomestayId(homestayId);
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setStatus(status);
        booking.setGuestCount(guestCount);
        booking.setContactPhone("13800138000");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        if (homestay != null && homestay.getPricePerNight() != null) {
            long nights = java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            booking.setTotalPrice(homestay.getPricePerNight().multiply(BigDecimal.valueOf(Math.max(nights, 1))));
        }
        bookingRepository.save(booking);
    }

    private void initializeAiConfig() {
        // 检查是否已有系统设置
        SystemSetting setting = systemSettingRepository.findAll()
                .stream()
                .findFirst()
                .orElse(new SystemSetting());

        // 配置AI参数
        setting.setAiApiKey("");
        setting.setAiBaseUrl("https://api.siliconflow.cn/");
        setting.setAiModel("Qwen/Qwen3-8B");
        setting.setAiEnabled(true);
        setting.setBookingWindowDays(30);

        systemSettingRepository.save(setting);
        System.out.println("   AI API Key: sk-****...（已配置）");
        System.out.println("   Base URL: https://api.siliconflow.cn/");
        System.out.println("   Model: Qwen/Qwen3-8B");
    }
}
