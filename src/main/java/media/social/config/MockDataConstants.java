package media.social.config;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MockDataConstants {

    private MockDataConstants() {}

    public static final List<String> MOCK_IMAGE_URLS = List.of(
            "https://res.cloudinary.com/dousreizx/image/upload/v1786763990/background_wdp99j.jpg",
            "https://res.cloudinary.com/dousreizx/image/upload/v1787309794/5df70f5f72391e5b725c5318b5ec8b8b_hunixe.jpg",
            "https://res.cloudinary.com/dousreizx/image/upload/v1787309796/12383ab3439574f4448baf4ca8129efb_gdbhfk.jpg",
            "https://res.cloudinary.com/dousreizx/image/upload/v1787309800/63e53b00d85f5705f69f2b913b8df62c_cqimb8.jpg",
            "https://res.cloudinary.com/dousreizx/image/upload/v1787309808/d80c57e2fb945552398f50aa4b3e0306_hgkuk9.jpg",
            "https://res.cloudinary.com/dousreizx/image/upload/v1786764146/avartar_hrbqwo.webp"
    );

    public static String getRandomImageUrl() {
        return MOCK_IMAGE_URLS.get(ThreadLocalRandom.current().nextInt(MOCK_IMAGE_URLS.size()));
    }

    public static final List<String> VIETNAMESE_STORY_CAPTIONS = List.of(
            "Hôm nay trời đẹp quá, dạo phố thôi nào! ☀️✨",
            "Cà phê sáng cùng bạn bè nạp năng lượng cho ngày mới ☕🥐",
            "Coding all night long, fix bug không ngủ được 💻🔥 #devlife",
            "Check-in cuối tuần chill chill cùng gia đình 🏖️🍹",
            "Mỗi ngày là một cơ hội mới để hoàn thiện bản thân 🌱💪",
            "Món ngon khó cưỡng! Ai đi ăn cùng không? 🍜😋",
            "Hoàng hôn tuyệt đẹp chiều nay bên bờ hồ 🌅🧡",
            "Gym time! Cháy hết mình vì một body săn chắc 🏋️‍♂️💯",
            "Hành trình vạn dặm bắt đầu từ một bước chân ✈️🎒",
            "Âm nhạc là liều thuốc chữa lành tâm hồn 🎧🎶",
            "Một ngày làm việc năng suất! Giờ là lúc thư giãn 🍕🎮",
            "Bình yên là khi tâm mình không bão giông 🍃🌸"
    );

    public static final List<String> VIETNAMESE_POST_CONTENTS = List.of(
            "Vừa hoàn thành dự án lớn cùng cả team sau bao đêm thức trắng! Cảm ơn mọi người đã luôn đồng hành và nỗ lực hết mình. Chúc mừng thành công của chúng ta! 🚀🎉 #teamwork #success #tech",
            "Cuối tuần rồi, cùng nhau đi trốn khỏi sự xô bồ của thành phố nào. Một tách cà phê thơm ngon và một cuốn sách hay là quá đủ cho một ngày bình yên. 🌿☕ #weekend #chill #peaceful",
            "Chia sẻ chút kinh nghiệm tối ưu hóa hiệu năng hệ thống với Spring Boot và Redis Cache. Anh em nào quan tâm thì để lại comment mình gửi tài liệu nhé! 💡💻 #springboot #redis #java #developer",
            "Hôm nay được trải nghiệm ẩm thực đường phố Hà Nội, công nhận bún chả và phở cuốn ở đây ngon đỉnh chóp! Có bạn nào mê ẩm thực giống mình không? 🍲🥢 #vietnamesefood #foodie #hanoi",
            "Chuyến đi Đà Lạt vừa rồi thật sự đáng nhớ! Không khí se lạnh, đồi thông bạt ngàn và những quán cafe view thung lũng cực thơ mộng. Nhất định sẽ quay lại sớm! 🌲🏕️ #dalat #travel #vietnam",
            "Tập luyện thể thao không chỉ giúp bạn khỏe hơn mỗi ngày mà còn rèn luyện tính kiên trì và kỷ luật. Đừng bỏ cuộc khi mọi thứ mới chỉ bắt đầu! 💪🏃‍♂️ #fitness #workout #motivation",
            "Cảm ơn cuộc đời vì mỗi sớm mai thức dậy lại có thêm một ngày nữa để yêu thương, cống hiến và sống trọn vẹn từng khoảnh khắc. Chúc cả nhà một ngày mới tốt lành! ☀️💐 #grateful #positivevibes",
            "Gặp lại những người bạn thân sau nhiều năm xa cách, mọi kỷ niệm thời thanh xuân ùa về như mới hôm qua. Tình bạn chân thành là món quà quý giá nhất! 🥂👥 #friends #memories #youth",
            "Review góc làm việc mới setup sau 3 ngày miệt mài dọn dẹp và trang trí. Đủ góc chill để vừa code vừa nghe nhạc thư giãn! 🖥️✨ #workspace #setup #minimalism",
            "Thế giới công nghệ AI đang phát triển với tốc độ chóng mặt. Học hỏi liên tục mỗi ngày là cách duy nhất để không bị tụt lại phía sau! 🤖🚀 #artificialintelligence #techtrends #learning"
    );

    public static final List<String> VIETNAMESE_COMMENTS = List.of(
            "Ảnh đẹp và chất lượng quá bạn ơi! 👏✨",
            "Tuyệt vời quá, chúc mừng bạn và team nhé! 🎉🚀",
            "Góc chụp đỉnh thật sự, view này ở đâu thế bạn? 😍",
            "Đồng quan điểm với bạn! Rất truyền cảm hứng! 💡💯",
            "Món này nhìn hấp dẫn quá chừng, xin địa chỉ quán với ạ 🤤🍜",
            "Chúc bạn một ngày tràn đầy năng lượng và niềm vui nhé! ☀️🌸",
            "Xuất sắc quá người anh em ơi! 🔥👍",
            "Chuẩn không cần chỉnh luôn nè! 💯👌",
            "Hóng bài chia sẻ tiếp theo của bạn nha! 📖",
            "Cố gắng lên nhé bạn, thành công sẽ đến sớm thôi! 💪❤️"
    );

    public static final List<String> GROUP_NAMES = List.of(
            "Hội Anh Em Lập Trình & Tech 💻",
            "Team Du Lịch & Khám Phá Việt Nam 🏖️",
            "Nhóm Bạn Thân Đại Học K20 🎓",
            "Hội Mê Đồ Ăn Ngon & Ẩm Thực 🍜",
            "Cộng Đồng Game Thủ Mobile & PC 🎮",
            "Hội Yêu Thích Gym & Fitness 💪",
            "Gia Đình & Người Thân Yêu ❤️",
            "Team Dự Án Khởi Nghiệp Công Nghệ 🚀",
            "CLB Tiếng Anh & Trao Đổi Ngôn Ngữ 🌍",
            "Hội Mê Nhiếp Ảnh & Phim Ảnh 📸"
    );

    public static final List<String> DATING_BIOS = List.of(
            "Thích du lịch, nghe nhạc indie và tìm kiếm một người có thể cùng ngồi cafe tán gẫu hàng giờ liền ☕✨",
            "Lập trình viên ban ngày, đầu bếp nghiệp dư ban đêm 🍳💻. Tìm một người cùng gu thưởng thức món ngon!",
            "Yêu thể thao, thích chạy bộ buổi sáng và leo núi cuối tuần 🏃‍♂️⛰️. Let's make memories together!",
            "Thích những điều giản dị, một buổi tối xem phim hay hoặc dạo bộ hóng gió ngắm thành phố về đêm 🎬🌃",
            "Người sống tích cực, hướng ngoại và luôn tràn đầy năng lượng ☀️. Hy vọng tìm được một nửa mảnh ghép hòa hợp!",
            "Thích đọc sách, chụp ảnh phim và du lịch trải nghiệm văn hóa mới 📷📚. Chờ đợi một người cùng tần số!"
    );

    public static final List<String> REPORT_REASONS = List.of(
            "Nội dung spam quảng cáo không phù hợp",
            "Ngôn từ gây thù ghét và xúc phạm cá nhân",
            "Hình ảnh hoặc video vi phạm tiêu chuẩn cộng đồng",
            "Tài khoản giả mạo người khác",
            "Nội dung sai lệch thông tin gây hiểu lầm",
            "Quấy rối hoặc đe dọa người dùng khác"
    );
}
