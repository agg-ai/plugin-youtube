package io.kestra.plugin.youtube.models;

public enum VideoCategory {
    film_animation("1", "Film & Animation"),
    autos_vehicles("2", "Autos & Vehicles"),
    music("10", "Music"),
    pets_animals("15", "Pets & Animals"),
    sports("17", "Sports"),
    short_movies("18", "Short Movies"),
    travel_events("19", "Travel & Events"),
    gaming("20", "Gaming"),
    videoblogging("21", "Videoblogging"),
    people_blogs("22", "People & Blogs"),
    comedy("23", "Comedy"),
    entertainment("24", "Entertainment"),
    news_politics("25", "News & Politics"),
    howto_style("26", "Howto & Style"),
    education("27", "Education"),
    science_technology("28", "Science & Technology"),
    nonprofits_activism("29", "Nonprofits & Activism"),
    movies("30", "Movies"),
    anime_animation("31", "Anime/Animation"),
    action_adventure("32", "Action/Adventure"),
    classics("33", "Classics"),
    comedy_2("34", "Comedy"),
    documentary("35", "Documentary"),
    drama("36", "Drama"),
    family("37", "Family"),
    foreign("38", "Foreign"),
    horror("39", "Horror"),
    scifi_fantasy("40", "Sci-Fi/Fantasy"),
    thriller("41", "Thriller"),
    shorts("42", "Shorts"),
    shows("43", "Shows"),
    trailers("44", "Trailers");

    private final String id;
    private final String title;

    VideoCategory(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
