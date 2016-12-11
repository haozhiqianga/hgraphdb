package io.hgraphdb.giraph;

import io.hgraphdb.HBaseBulkLoader;
import io.hgraphdb.HBaseEdge;
import io.hgraphdb.HBaseGraph;
import io.hgraphdb.HBaseVertex;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.io.EdgeOutputFormat;
import org.apache.giraph.io.EdgeWriter;
import org.apache.hadoop.hbase.mapreduce.TableOutputCommitter;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

/**
 * Base class for writing Edge mutations back to specific
 * rows in an HBase table. This class wraps an instance of TableOutputFormat
 * for easy configuration with the existing properties.
 * <p>
 * Setting conf.set(TableOutputFormat.OUTPUT_TABLE, "out_table");
 * will properly delegate to the TableOutputFormat instance contained
 * in this class. The Configurable interface prevents specific
 * wrapper methods from having to be called.
 * <p>
 * Works with {@link HBaseEdgeInputFormat}
 *
 */
@SuppressWarnings("rawtypes")
public abstract class HBaseEdgeOutputFormat
        extends EdgeOutputFormat<ObjectWritable, VertexWritable, EdgeWritable> {

    /**
     * Constructor
     * <p>
     * Simple class which takes an instance of RecordWriter
     * over Writable objects. Subclasses are
     * expected to implement writeEdge()
     *
     */
    public abstract static class HBaseEdgeWriter
            extends EdgeWriter<ObjectWritable, VertexWritable, EdgeWritable> {

        /**
         * HBase graph
         */
        private final HBaseGraph graph;

        /**
         * Context
         */
        private TaskAttemptContext context;

        /**
         * Bulk loader
         */
        private HBaseBulkLoader writer;

        /**
         * Sets up base table output format and creates a record writer.
         *
         * @param context task attempt context
         */
        public HBaseEdgeWriter(TaskAttemptContext context)
                throws IOException, InterruptedException {
            this.writer = new HBaseBulkLoader(context.getConfiguration());
            this.graph = writer.getGraph();
        }

        /**
         * initialize
         *
         * @param context Context used to write the vertices.
         * @throws IOException
         */
        public void initialize(TaskAttemptContext context)
                throws IOException {
            this.context = context;
        }

        /**
         * close
         *
         * @param context the context of the task
         * @throws IOException
         * @throws InterruptedException
         */
        public void close(TaskAttemptContext context)
                throws IOException, InterruptedException {
            writer.close();
        }

        /**
         * Get the writer
         *
         * @return Bulk loader to be used for writing.
         */
        public HBaseBulkLoader getWriter() {
            return writer;
        }

        /**
         * getContext
         *
         * @return Context passed to initialize.
         */
        public TaskAttemptContext getContext() {
            return context;
        }

        @Override
        public void writeEdge(
                ObjectWritable id,
                VertexWritable vertex,
                Edge<ObjectWritable, EdgeWritable> edge)
                throws IOException, InterruptedException {
            HBaseVertex v = vertex.get();
            v.setGraph(graph);
            HBaseEdge e = edge.getValue().get();
            e.setGraph(graph);
            ((HBaseVertex) e.outVertex()).setGraph(graph);
            ((HBaseVertex) e.inVertex()).setGraph(graph);
            writeEdge(getWriter(), v, e);
        }

        public abstract void writeEdge(HBaseBulkLoader writer, HBaseVertex outVertex, HBaseEdge edge);
    }

    /**
     * checkOutputSpecs
     *
     * @param context information about the job
     * @throws IOException
     * @throws InterruptedException
     */
    public void checkOutputSpecs(JobContext context)
            throws IOException, InterruptedException {
    }

    /**
     * getOutputCommitter
     *
     * @param context the task context
     * @return OutputCommitter ouputCommitter
     * @throws IOException
     * @throws InterruptedException
     */
    public OutputCommitter getOutputCommitter(
            TaskAttemptContext context)
            throws IOException, InterruptedException {
        return new TableOutputCommitter();
    }
}
