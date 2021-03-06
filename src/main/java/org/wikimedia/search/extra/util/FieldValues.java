package org.wikimedia.search.extra.util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.fieldvisitor.CustomFieldsVisitor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Hub for fetching field values.
 */
public abstract class FieldValues {
    /**
     * Loads field values.
     */
    public interface Loader {
        /**
         * Load the value of the string at path from reader for docId.
         */
        List<String> load(String path, IndexReader reader, int docId) throws IOException;
    }

    /**
     * Load field values from source. Note that values aren't cached between
     * calls so providing the same arguments over and over again would call down
     * into Lucene every time.
     */
    public static FieldValues.Loader loadFromSource() {
        return Source.INSTANCE;
    }

    /**
     * Load field values from a stored field. Note that values aren't cached
     * between calls so providing the same arguments over and over again would
     * call down into Lucene every time.
     */
    public static FieldValues.Loader loadFromStoredField() {
        return Stored.INSTANCE;
    }

    /**
     * Wraps loaded field values in a transforming list.
     */
    public static FieldValues.Loader transform(FieldValues.Loader next, Function<String, String> transformer) {
        return new Transformed(next, transformer);
    }

    private FieldValues() {
        // Util class
    }

    private static class Source implements FieldValues.Loader {
        private static final FieldValues.Loader INSTANCE = new Source();
        @Override
        public List<String> load(String path, IndexReader reader, int docId) throws IOException {
            CustomFieldsVisitor visitor = new CustomFieldsVisitor(Collections.<String>emptySet(), true);
            reader.document(docId, visitor);
            BytesReference source = visitor.source();
            Map<String, Object> map = XContentHelper.convertToMap(source, false).v2();
            return Lists.transform(XContentMapValues.extractRawValues(path, map), Functions.toStringFunction());
        }
    }

    private static class Stored implements FieldValues.Loader {
        private static final FieldValues.Loader INSTANCE = new Stored();
        @Override
        public List<String> load(String path, IndexReader reader, int docId) throws IOException {
            CustomFieldsVisitor visitor = new CustomFieldsVisitor(ImmutableSet.of(path), false);
            reader.document(docId, visitor);
            return Lists.transform(visitor.fields().get(path), Functions.toStringFunction());
        }
    }

    private static class Transformed implements FieldValues.Loader {
        private final FieldValues.Loader next;
        private final Function<String, String> transformer;

        public Transformed(FieldValues.Loader next, Function<String, String> transformer) {
            this.next = next;
            this.transformer = transformer;
        }

        @Override
        public List<String> load(String path, IndexReader reader, int docId) throws IOException {
            return Lists.transform(next.load(path, reader, docId), transformer);
        }
    }
}
