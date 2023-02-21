package eu.nonstatic.cue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Getter
public class CueIssues implements Iterable<CueIssues.Entry> {
    private final List<Entry> list = new ArrayList<>();

    public void add(String message) {
        add(message, null);
    }

    public void add(Throwable throwable) {
        add(null, throwable);
    }

    public void add(String message, Throwable throwable) {
        if(message != null || throwable != null) {
            list.add(new Entry(message, throwable));
        }
    }

    public void addAll(CueIssues cueIssues) {
        this.list.addAll(cueIssues.list);
    }

    @Override
    public Iterator<Entry> iterator() {
        return list.iterator();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public IllegalStateException toException() {
        IllegalStateException ex = new IllegalStateException("Issues found in Cue description");
        list.forEach(issue -> ex.addSuppressed(issue.toException()));
        return ex;
    }

    @Getter
    @AllArgsConstructor
    public static class Entry {
        private final String message;
        private final Throwable throwable;


        public Throwable toException() {
            return Optional.ofNullable(throwable).orElseGet(() -> new IllegalStateException(message));
        }
    }

}
