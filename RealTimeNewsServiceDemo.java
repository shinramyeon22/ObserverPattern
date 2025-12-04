import java.time.LocalDateTime;
import java.util.*;

record News(
        UUID id,
        String title,
        String content,
        String category,
        Priority priority,
        LocalDateTime publishedAt
) {
    public enum Priority {
        BREAKING, URGENT, NORMAL
    }

    public News(String title, String content, String category, Priority priority) {
        this(UUID.randomUUID(), title, content, category.toLowerCase(), priority, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - %s",
                priority, title, category, publishedAt.toLocalTime());
    }
}

interface NewsSubscriber {
    void update(News news);
    String getSubscriberId();
}

class EmailSubscriber implements NewsSubscriber {
    private final String name;
    private final String email;
    private final Set<String> interestedCategories;
    private final News.Priority minPriority;

    public EmailSubscriber(String name, String email, Set<String> categories, News.Priority minPriority) {
        this.name = name;
        this.email = email;
        this.interestedCategories = Set.copyOf(categories);
        this.minPriority = minPriority != null ? minPriority : News.Priority.NORMAL;
    }

    public EmailSubscriber(String name, String email) {
        this(name, email, Set.of("general"), News.Priority.NORMAL);
    }

    @Override
    public void update(News news) {
        boolean interested = interestedCategories.contains("general") ||
                interestedCategories.contains(news.category());

        boolean priorityOk = news.priority().ordinal() <= minPriority.ordinal();

        if (interested && priorityOk) {
            System.out.printf("EMAIL To: %s <%s>%n", name, email);
            System.out.printf("      Subject: %s: %s%n", news.priority(), news.title());
            System.out.printf("      %s%n%n", news);
        }
    }

    @Override
    public String getSubscriberId() {
        return "email:" + email;
    }

    @Override
    public String toString() {
        return "EmailSubscriber[" + name + "]";
    }
}

class MobileAppSubscriber implements NewsSubscriber {
    private final String userId;
    private final String deviceToken;
    private final Set<String> interests;

    public MobileAppSubscriber(String userId, String deviceToken, Set<String> interests) {
        this.userId = userId;
        this.deviceToken = deviceToken;
        this.interests = interests.isEmpty() ? Set.of("general") : Set.copyOf(interests);
    }

    public MobileAppSubscriber(String userId, String deviceToken) {
        this(userId, deviceToken, Set.of("general"));
    }

    @Override
    public void update(News news) {
        if (interests.contains("general") || interests.contains(news.category())) {
            System.out.printf("PUSH User %s (Device: %s...)%n",
                    userId, deviceToken.length() >= 8 ? deviceToken.substring(0, 8) : deviceToken);
            System.out.printf("     %s%n%n", news);
        }
    }

    @Override
    public String getSubscriberId() {
        return "mobile:" + userId;
    }

    @Override
    public String toString() {
        return "MobileAppSubscriber[" + userId + "]";
    }
}

class NewsAgency {
    private final String name;
    private final Map<String, NewsSubscriber> subscribers = new HashMap<>();

    public NewsAgency(String name) {
        this.name = name;
    }

    public synchronized void subscribe(NewsSubscriber subscriber) {
        String id = subscriber.getSubscriberId();
        if (!subscribers.containsKey(id)) {
            subscribers.put(id, subscriber);
            System.out.println("[SUBSCRIBED] " + subscriber + " to " + name);
        }
    }

    public synchronized void unsubscribe(NewsSubscriber subscriber) {
        String id = subscriber.getSubscriberId();
        if (subscribers.remove(id) != null) {
            System.out.println("[UNSUBSCRIBED] " + subscriber + " left " + name + "\n");
        }
    }

    private void notifySubscribers(News news) {
        System.out.println("\n" + name + " PUBLISHING " + news);
        System.out.println("-".repeat(70));
        new ArrayList<>(subscribers.values()).forEach(s -> s.update(news));
        System.out.println("-".repeat(70));
    }

    public void publishNews(String title, String content, String category, News.Priority priority) {
        News news = new News(title, content, category, priority);
        notifySubscribers(news);
    }

    public void publishNews(String title, String content) {
        publishNews(title, content, "general", News.Priority.NORMAL);
    }

    public int getSubscriberCount() {
        return subscribers.size();
    }
}

public class RealTimeNewsServiceDemo {
    public static void main(String[] args) {
        NewsAgency agency = new NewsAgency("Global News Network (GNN)");

        var alice = new EmailSubscriber("Alice Johnson", "alice@example.com",
                Set.of("politics", "technology"), News.Priority.NORMAL);

        var bob = new MobileAppSubscriber("user_789", "abc123xyz789");

        var carol = new EmailSubscriber("Carol Davis", "carol@work.com",
                Set.of("sports"), News.Priority.NORMAL);

        var dave = new MobileAppSubscriber("user_456", "def456uvw123",
                Set.of("technology"));

        agency.subscribe(alice);
        agency.subscribe(bob);
        agency.subscribe(carol);
        agency.subscribe(dave);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("BREAKING NEWS");
        System.out.println("=".repeat(80));
        agency.publishNews("Major Earthquake Hits Pacific Coast",
                "A 7.8 magnitude earthquake struck at 14:32 local time...",
                "breaking", News.Priority.BREAKING);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("SPORTS NEWS");
        System.out.println("=".repeat(80));
        agency.publishNews("National Team Wins Championship!",
                "Historic victory after 20 years.", "sports", News.Priority.NORMAL);

        System.out.println("\nBob unsubscribes...");
        agency.unsubscribe(bob);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TECHNOLOGY NEWS");
        System.out.println("=".repeat(80));
        agency.publishNews("Quantum Computing Breakthrough Achieved",
                "Scientists successfully demonstrate stable 100-qubit system.",
                "technology", News.Priority.URGENT);

        System.out.println("Active subscribers: " + agency.getSubscriberCount());
    }
}