package com.gft.dlp.model;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;

public class Job implements Serializable {

    //private static transient DateTimeFormatter timeFormatter =
      //      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.US).withZoneUTC();

    public String description;
    public float salary;

    public Job() {

    }

    public Job(String description, float salary) {
        this.description = description;
        this.salary = salary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public float getSalary() {
        return salary;
    }

    public void setSalary(float salary) {
        this.salary = salary;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static Job fromJson(String line) {
        ObjectMapper mapper = new ObjectMapper();
        Job obj = null;
        try {
            obj =  mapper.readValue(line, Job.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return obj;
    }
    public static class ConvertFunction implements MapFunction<String, Job> {

        @Override
        public Job map(String s) throws Exception {
            return Job.fromJson(s);
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(description).append(",");
        sb.append(salary);

        return sb.toString();
    }
}