package io.kestra.plugin.youtube.models;

public enum VideoCategory {
    FILM_ANIMATION("1", "Film & Animation"),
    AUTOS_VEHICLES("2", "Autos & Vehicles"),
    MUSIC("10", "Music"),
    PETS_ANIMALS("15", "Pets & Animals"),
    SPORTS("17", "Sports"),
    SHORT_MOVIES("18", "Short Movies"),
    TRAVEL_EVENTS("19", "Travel & Events"),
    GAMING("20", "Gaming"),
    VIDEOBLOGGING("21", "Videoblogging"),
    PEOPLE_BLOGS("22", "People & Blogs"),
    COMEDY("23", "Comedy"),
    ENTERTAINMENT("24", "Entertainment"),
    NEWS_POLITICS("25", "News & Politics"),
    HOWTO_STYLE("26", "Howto & Style"),
    EDUCATION("27", "Education"),
    SCIENCE_TECHNOLOGY("28", "Science & Technology"),
    NONPROFITS_ACTIVISM("29", "Nonprofits & Activism"),
    MOVIES("30", "Movies"),
    ANIME_ANIMATION("31", "Anime/Animation"),
    ACTION_ADVENTURE("32", "Action/Adventure"),
    CLASSICS("33", "Classics"),
    COMEDY_2("34", "Comedy"),
    DOCUMENTARY("35", "Documentary"),
    DRAMA("36", "Drama"),
    FAMILY("37", "Family"),
    FOREIGN("38", "Foreign"),
    HORROR("39", "Horror"),
    SCIFI_FANTASY("40", "Sci-Fi/Fantasy"),
    THRILLER("41", "Thriller"),
    SHORTS("42", "Shorts"),
    SHOWS("43", "Shows"),
    TRAILERS("44", "Trailers");

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
