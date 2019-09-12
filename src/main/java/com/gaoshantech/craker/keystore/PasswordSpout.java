package com.gaoshantech.craker.keystore;

import com.google.common.primitives.Chars;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichSpout;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PasswordSpout implements IRichSpout {
    private SpoutOutputCollector collector;
    private String passwordMask;
    private static List<Character> ASCII_LOWERCASE = Chars.asList("abcdefghijklmnopqrstuvwxyz".toCharArray());
    private static List<Character> ASCII_UPPERCASE = Chars.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray());
    private static List<Character> ASCII_LETTERS = Chars.asList("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray());
    private static List<Character> DIGITS = Chars.asList("0123456789".toCharArray());
    private static List<Character> PUNCTUATION = Chars.asList("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ ".toCharArray());
    private static List<Character> PUNCTUATION_TINY = Chars.asList("!@#$%^&*()-_+= ".toCharArray());
    private static List<Character> PRINTABLE = Chars.asList("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray());

    public PasswordSpout(String passwordMask){
        this.passwordMask = passwordMask;
    }
    @Override
    public void open(Map<String, Object> map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.collector = spoutOutputCollector;
    }

    @Override
    public void close() {

    }

    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

    }

    @Override
    public void nextTuple() {
        ArrayList<ArrayList<Character>> passwordGroups = new ArrayList<>();
        for (int i = 0; i < passwordMask.length(); i++) {
            char ch = passwordMask.charAt(i);
            /**
             * %%	- static character %
             * %c	- lower-case Latin characters  (a..z)
             * %C	- upper-case Latin characters (A..Z)
             * %w	- Latin characters (a..Z)
             * %#	- full set of special characters (!..~ space)
             * %@	- small set of special characters (!@#$%^&*()-_+= space)
             * %?	- all printable characters with ASCII codes of 32..127
             * %d	- one digit (0..9)
             */
            if(ch == '%' && (i - 1) < passwordMask.length()){
                ArrayList<Character> group = new ArrayList<>();
                switch (passwordMask.charAt(i+1)){
                    case '%':
                        group.add(ch);
                        i++;
                        break;
                    case 'c':
                        group.addAll(ASCII_LOWERCASE);
                        i++;
                        break;
                    case 'C':
                        group.addAll(ASCII_UPPERCASE);
                        i++;
                        break;
                    case 'w':
                        group.addAll(ASCII_LETTERS);
                        i++;
                        break;
                    case '#':
                        group.addAll(PUNCTUATION);
                        i++;
                        break;
                    case '@':
                        group.addAll(PUNCTUATION_TINY);
                        i++;
                        break;
                    case '?':
                        group.addAll(PRINTABLE);
                        i++;
                        break;
                    case 'd':
                        group.addAll(DIGITS);
                        i++;
                        break;
                }
                passwordGroups.add(group);
            }
            else {
                ArrayList<Character> group = new ArrayList<>(1);
                group.add(ch);
                passwordGroups.add(group);
            }
        }
        Stream<String> stream = IntStream.range(0, 1).mapToObj(i -> "").parallel();
        for (ArrayList<Character> group : passwordGroups) {
            stream = stream.map(item-> group.stream().map(str->item+str).collect(Collectors.toList())).flatMap(Collection::stream);
        }
        stream.forEach(password->{
            this.collector.emit(new Values(password));
        });
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void ack(Object o) {
    }

    @Override
    public void fail(Object o) {
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("password"));
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
}
