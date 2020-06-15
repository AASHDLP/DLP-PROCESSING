package com.gft.dlp.model;

/*
  Clase que utiliza los mismos nombres que el json de respuesta de Twitter para poder tratar los campos del
  JSON directamente con el objeto
 */
public class Tweet {
    // id del tweet
    private long id;
    // mensaje del tweet
    private String text;
    private boolean truncated =false;
    // fecha de creación del tweet (event time)
    private String created_at;

    public Tweet(long id, String text, boolean truncated, String created_at) {
        this.id = id;
        this.truncated=truncated;
        this.created_at=created_at;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public void setId(long myid) {
        this.id = myid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


    @Override
    public String toString() {
        return "Tweet{" +
                "id=" + id +
                ", text='" + text +
                ", created_at='" + created_at +
                ", truncated='" + truncated + '\'' +
                '}';
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

}
