package com.qoomon.logback;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;

import javax.net.SocketFactory;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ch.qos.logback.core.net.SocketConnector;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.util.CloseUtil;

/**
 * An abstract base for module specific {@code SocketAppender} implementations in other logback modules.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 * @author Carl Harris
 */

public abstract class AbstractSocketAppender<E> extends AppenderBase<E>
        implements Runnable, SocketConnector.ExceptionHandler
{

    /**
     * The default port number of remote logging server (4560).
     */
    public static final int  DEFAULT_PORT                    = 4560;

    /**
     * The default reconnection delay (30000 milliseconds or 30 seconds).
     */
    public static final int  DEFAULT_RECONNECTION_DELAY      = 30000;

    /**
     * Default size of the queue used to hold logging events that are destined for the remote peer.
     */
    public static final int  DEFAULT_QUEUE_SIZE              = 0;

    /**
     * Default timeout when waiting for the remote server to accept our connection.
     */
    private static final int DEFAULT_ACCEPT_CONNECTION_DELAY = 5000;

    /**
     * It is the encoder which is ultimately responsible for writing the event to an {@link OutputStream}.
     */
    protected Encoder<E>     encoder;

    private String           remoteHost;
    private int              port                            = DEFAULT_PORT;
    private InetAddress      address;
    private int              reconnectionDelay               = DEFAULT_RECONNECTION_DELAY;
    private int              queueSize                       = DEFAULT_QUEUE_SIZE;
    private int              acceptConnectionTimeout         = DEFAULT_ACCEPT_CONNECTION_DELAY;

    private BlockingQueue<E> queue;
    private String           peerId;
    private Future<?>        task;
    private Future<Socket>   connectorTask;

    private volatile Socket  socket;




    /**
     * Constructs a new appender.
     */
    protected AbstractSocketAppender()
    {
    }




    /**
     * Constructs a new appender that will connect to the given remote host and port.
     * <p>
     * This constructor was introduced primarily to allow the encapsulation of the this class to be improved in a manner that is least disruptive to <em>existing</em> subclasses. <strong>This constructor will be removed in future release</strong>.
     *
     * @param remoteHost
     *            target remote host
     * @param port
     *            target port on remote host
     */
    @Deprecated
    protected AbstractSocketAppender(final String remoteHost, final int port)
    {
        this.remoteHost = remoteHost;
        this.port = port;
    }




    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
    {
        if (this.isStarted())
        {
            return;
        }
        int errorCount = 0;

        if (this.encoder == null)
        {
            this.addError("No encoder set for the appender named \"" + this.name + "\".");
            errorCount++;
        }

        if (this.port <= 0)
        {
            errorCount++;
            this.addError("No port was configured for appender"
                    + this.name
                    + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_port");
        }

        if (this.remoteHost == null)
        {
            errorCount++;
            this.addError("No remote host was configured for appender"
                    + this.name
                    + " For more information, please visit http://logback.qos.ch/codes.html#socket_no_host");
        }

        if (this.queueSize < 0)
        {
            errorCount++;
            this.addError("Queue size must be non-negative");
        }

        if (errorCount == 0)
        {
            try
            {
                this.address = InetAddress.getByName(this.remoteHost);
            }
            catch (final UnknownHostException ex)
            {
                this.addError("unknown host: " + this.remoteHost);
                errorCount++;
            }
        }

        if (errorCount == 0)
        {
            this.queue = this.newBlockingQueue(this.queueSize);
            this.peerId = "remote peer " + this.remoteHost + ":" + this.port + ": ";
            this.task = this.getContext().getExecutorService().submit(this);
            super.start();
        }
    }




    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (!this.isStarted())
        {
            return;
        }
        this.encoderClose();
        CloseUtil.closeQuietly(this.socket);
        this.task.cancel(true);
        if (this.connectorTask != null)
        {
            this.connectorTask.cancel(true);
        }
        super.stop();
    }




    void encoderClose()
    {
        if (this.encoder != null && this.socket != null)
        {
            try
            {
                this.encoder.close();
            }
            catch (final IOException ioe)
            {
                this.started = false;
                this.addStatus(new ErrorStatus("Failed to write footer for appender named ["
                        + this.name + "].", this, ioe));
            }
        }
    }




    void encoderInit()
    {
        if (this.encoder != null && this.socket != null)
        {
            try
            {
                this.encoder.init(this.socket.getOutputStream());
            }
            catch (final IOException ioe)
            {
                this.started = false;
                this.addStatus(new ErrorStatus(
                        "Failed to initialize encoder for appender named [" + this.name + "].",
                        this, ioe));
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void append(final E event)
    {
        if (event == null || !this.isStarted())
        {
            return;
        }
        this.queue.offer(event);
    }




    /**
     * {@inheritDoc}
     */
    @Override
    public final void run()
    {
        try
        {
            while (!Thread.currentThread().isInterrupted())
            {
                final SocketConnector connector = this.createConnector(this.address, this.port, 0,
                        this.reconnectionDelay);

                this.connectorTask = this.activateConnector(connector);
                if (this.connectorTask == null)
                {
                    break;
                }

                this.socket = this.waitForConnectorToReturnASocket();
                if (this.socket == null)
                {
                    break;
                }
                this.dispatchEvents();
            }
        }
        catch (final InterruptedException ex)
        {
            assert true; // ok... we'll exit now
        }
        this.addInfo("shutting down");
    }




    private SocketConnector createConnector(final InetAddress address, final int port,
            final int initialDelay, final int retryDelay)
    {
        final SocketConnector connector = this.newConnector(address, port, initialDelay,
                retryDelay);
        connector.setExceptionHandler(this);
        connector.setSocketFactory(this.getSocketFactory());
        return connector;
    }




    private Future<Socket> activateConnector(final SocketConnector connector)
    {
        try
        {
            return this.getContext().getExecutorService().submit(connector);
        }
        catch (final RejectedExecutionException ex)
        {
            return null;
        }
    }




    private Socket waitForConnectorToReturnASocket() throws InterruptedException
    {
        try
        {
            final Socket s = this.connectorTask.get();
            this.connectorTask = null;
            return s;
        }
        catch (final ExecutionException e)
        {
            return null;
        }
    }




    private void dispatchEvents() throws InterruptedException
    {
        try
        {
            this.socket.setSoTimeout(this.acceptConnectionTimeout);
            this.encoderInit();
            this.socket.setSoTimeout(0);
            this.addInfo(this.peerId + "connection established");
            while (true)
            {
                final E event = this.queue.take();
                this.postProcessEvent(event);
                this.encoder.doEncode(event);
            }
        }
        catch (final IOException ex)
        {
            this.addInfo(this.peerId + "connection failed: " + ex);
        }
        finally
        {
            CloseUtil.closeQuietly(this.socket);
            this.socket = null;
            this.addInfo(this.peerId + "connection closed");
        }
    }




    /**
     * {@inheritDoc}
     */
    @Override
    public void connectionFailed(final SocketConnector connector, final Exception ex)
    {
        if (ex instanceof InterruptedException)
        {
            this.addInfo("connector interrupted");
        }
        else if (ex instanceof ConnectException)
        {
            this.addInfo(this.peerId + "connection refused");
        }
        else
        {
            this.addInfo(this.peerId + ex);
        }
    }




    /**
     * Creates a new {@link SocketConnector}.
     * <p>
     * The default implementation creates an instance of {@link DefaultSocketConnector}. A subclass may override to provide a different {@link SocketConnector} implementation.
     *
     * @param address
     *            target remote address
     * @param port
     *            target remote port
     * @param initialDelay
     *            delay before the first connection attempt
     * @param retryDelay
     *            delay before a reconnection attempt
     * @return socket connector
     */
    protected SocketConnector newConnector(final InetAddress address,
            final int port, final int initialDelay, final int retryDelay)
    {
        return new DefaultSocketConnector(address, port, initialDelay, retryDelay);
    }




    /**
     * Gets the default {@link SocketFactory} for the platform.
     * <p>
     * Subclasses may override to provide a custom socket factory.
     */
    protected SocketFactory getSocketFactory()
    {
        return SocketFactory.getDefault();
    }




    /**
     * Creates a blocking queue that will be used to hold logging events until they can be delivered to the remote receiver.
     * <p>
     * The default implementation creates a (bounded) {@link ArrayBlockingQueue} for positive queue sizes. Otherwise it creates a {@link SynchronousQueue}.
     * <p>
     * This method is exposed primarily to support instrumentation for unit testing.
     *
     * @param queueSize
     *            size of the queue
     * @return
     */
    BlockingQueue<E> newBlockingQueue(final int queueSize)
    {
        return queueSize <= 0 ?
                new SynchronousQueue<E>() : new ArrayBlockingQueue<E>(queueSize);
    }




    /**
     * Post-processes an event before it is serialized for delivery to the remote receiver.
     *
     * @param event
     *            the event to post-process
     */
    protected abstract void postProcessEvent(E event);




    /*
     * This method is used by logback modules only in the now deprecated convenience constructors for SocketAppender
     */
    @Deprecated
    protected static InetAddress getAddressByName(final String host)
    {
        try
        {
            return InetAddress.getByName(host);
        }
        catch (final Exception e)
        {
            // addError("Could not find address of [" + host + "].", e);
            return null;
        }
    }




    /**
     * The <b>RemoteHost</b> property takes the name of of the host where a corresponding server is running.
     */
    public void setRemoteHost(final String host)
    {
        this.remoteHost = host;
    }




    /**
     * Returns value of the <b>RemoteHost</b> property.
     */
    public String getRemoteHost()
    {
        return this.remoteHost;
    }




    /**
     * The <b>Port</b> property takes a positive integer representing the port where the server is waiting for connections.
     */
    public void setPort(final int port)
    {
        this.port = port;
    }




    /**
     * Returns value of the <b>Port</b> property.
     */
    public int getPort()
    {
        return this.port;
    }




    /**
     * The <b>reconnectionDelay</b> property takes a positive integer representing the number of milliseconds to wait between each failed connection attempt to the server. The default value of this option is 30000 which corresponds to 30 seconds.
     *
     * <p>
     * Setting this option to zero turns off reconnection capability.
     */
    public void setReconnectionDelay(final int delay)
    {
        this.reconnectionDelay = delay;
    }




    /**
     * Returns value of the <b>reconnectionDelay</b> property.
     */
    public int getReconnectionDelay()
    {
        return this.reconnectionDelay;
    }




    /**
     * The <b>queueSize</b> property takes a non-negative integer representing the number of logging events to retain for delivery to the remote receiver. When the queue size is zero, event delivery to the remote receiver is synchronous. When the queue
     * size is greater than zero, the {@link #append(Object)} method returns immediately after enqueing the event, assuming that there is space available in the queue. Using a non-zero queue length can improve performance by eliminating delays caused by
     * transient network delays. If the queue is full when the {@link #append(Object)} method is called, the event is summarily and silently dropped.
     *
     * @param queueSize
     *            the queue size to set.
     */
    public void setQueueSize(final int queueSize)
    {
        this.queueSize = queueSize;
    }




    /**
     * Returns the value of the <b>queueSize</b> property.
     */
    public int getQueueSize()
    {
        return this.queueSize;
    }




    /**
     * Sets the timeout that controls how long we'll wait for the remote peer to accept our connection attempt.
     * <p>
     * This property is configurable primarily to support instrumentation for unit testing.
     *
     * @param acceptConnectionTimeout
     *            timeout value in milliseconds
     */
    void setAcceptConnectionTimeout(final int acceptConnectionTimeout)
    {
        this.acceptConnectionTimeout = acceptConnectionTimeout;
    }




    public Encoder<E> getEncoder()
    {
        return this.encoder;
    }




    public void setEncoder(final Encoder<E> encoder)
    {
        this.encoder = encoder;
    }

}
