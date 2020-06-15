package com.gft.dlp.model;

import com.google.gson.JsonObject;

public class TweetExtended {
    private JsonObject extended_tweet;

     public TweetExtended(JsonObject extended_tweet) {
        this.extended_tweet = extended_tweet;
    }

    public JsonObject getExtended_tweet() {
        return extended_tweet;
    }

    public void setExtended_tweet(JsonObject extended_tweet) {
        this.extended_tweet = extended_tweet;
    }
}
