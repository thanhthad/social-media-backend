package media.social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.notification.entity.Notification;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.repository.NotificationRepository;
import media.social.modules.user.entity.User;
import media.social.modules.user.repository.UserRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMockService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final Faker faker = new Faker(new Locale("vi"));

    @Transactional
    public void init() {
        if (notificationRepository.count() >= 1000) {
            log.info("Notifications already initialized");
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return;

        log.info("Generating Notifications...");
        List<Notification> notificationsBatch = new ArrayList<>();
        EntityType[] eTypes = EntityType.values();
        NotificationType[] nTypes = NotificationType.values();

        int batchSize = 100;
        for (User receiver : users) {
            int notifCount = faker.number().numberBetween(5, 20);
            for (int i = 0; i < notifCount; i++) {
                User sender = users.get(faker.number().numberBetween(0, users.size()));
                if (receiver.getId().equals(sender.getId())) continue;

                Notification notification = Notification.builder()
                        .receiver(receiver)
                        .sender(sender)
                        .entityType(eTypes[faker.number().numberBetween(0, eTypes.length)])
                        .entityId((long) faker.number().numberBetween(1, 1000))
                        .type(nTypes[faker.number().numberBetween(0, nTypes.length)])
                        .isRead(faker.bool().bool())
                        .createdAt(LocalDateTime.now().minusDays(faker.number().numberBetween(0, 30)))
                        .build();

                notificationsBatch.add(notification);

                if (notificationsBatch.size() == batchSize) {
                    notificationRepository.saveAll(notificationsBatch);
                    notificationsBatch.clear();
                }
            }
        }
        
        if (!notificationsBatch.isEmpty()) {
            notificationRepository.saveAll(notificationsBatch);
        }

        log.info("Notification generation complete");
    }
}
