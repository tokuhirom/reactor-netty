/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.ipc.netty.options;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.security.cert.CertificateException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.NetUtil;
import reactor.core.Exceptions;

/**
 * Encapsulates configuration options for server connectors.
 *
 * @author Stephane Maldini
 */
public class ServerOptions extends NettyOptions<ServerOptions> {

	/**
	 * Create a new server builder
	 * @return a new server builder
	 */
	public static ServerOptions create() {
		return new ServerOptions().daemon(false);
	}

	/**
	 * Create a server builder for listening on the current default localhost address
	 * (inet4 or inet6) and {@code port}.
	 * @param port the port to listen on.
	 * @return a new server builder
	 */
	public static ServerOptions on(int port) {
		return on(NetUtil.LOCALHOST.getHostAddress(), port);
	}
	/**
	 * Create a server builder for listening on the given {@code bindAddress} and port
	 * 0 which will resolve to an available port.
	 * @param bindAddress the hostname to be resolved into an address to listen
	 * @return a new server builder
	 */
	public static ServerOptions on(String bindAddress) {
		return on(bindAddress, 0);
	}

	/**
	 * Create a server builder for listening on the given {@code bindAddress} and
	 * {@code port}.
	 * @param bindAddress the hostname to be resolved into an address to listen
	 * @param port the port to listen on.
	 * @return a new server builder
	 */
	public static ServerOptions on(String bindAddress, int port) {
		return new ServerOptions().listen(bindAddress, port)
		                          .daemon(false);
	}

	int            backlog        = 1000;
	boolean        reuseAddr      = true;

	ProtocolFamily protocolFamily = null;
	InetSocketAddress listenAddress;
	NetworkInterface  multicastInterface;

	ServerOptions(){
	}

	ServerOptions(ServerOptions options){
		super(options);
		this.listenAddress = options.listenAddress;
		this.multicastInterface = options.multicastInterface;
		this.backlog = options.backlog;
		this.reuseAddr = options.reuseAddr;
		this.protocolFamily = options.protocolFamily;
	}

	/**
	 * Returns the configured pending connection backlog for the socket.
	 *
	 * @return The configured connection backlog size
	 */
	public int backlog() {
		return backlog;
	}

	/**
	 * Configures the size of the pending connection backlog for the socket.
	 *
	 * @param backlog The size of the backlog
	 * @return {@code this}
	 */
	public ServerOptions backlog(int backlog) {
		this.backlog = backlog;
		return this;
	}

	@Override
	public ServerOptions duplicate() {
		return new ServerOptions(this);
	}

	/**
	 * The port on which this server should listen, assuming it should bind to all available addresses.
	 *
	 * @param port The port to listen on.
	 * @return {@literal this}
	 */
	public ServerOptions listen(int port) {
		return listen(new InetSocketAddress(port));
	}

	/**
	 * The host and port on which this server should listen.
	 *
	 * @param host The host to bind to.
	 * @param port The port to listen on.
	 * @return {@literal this}
	 */
	public ServerOptions listen(String host, int port) {
		if (null == host) {
			host = "localhost";
		}
		return listen(new InetSocketAddress(host, port));
	}

	/**
	 * The {@link InetSocketAddress} on which this server should listen.
	 *
	 * @param listenAddress the listen address
	 * @return {@literal this}
	 */
	public ServerOptions listen(InetSocketAddress listenAddress) {
		this.listenAddress = listenAddress;
		return this;
	}


	/**
	 * Return the listening {@link InetSocketAddress}
	 * @return the listening address
	 */
	public InetSocketAddress listenAddress(){
		return this.listenAddress;
	}

	/**
	 * Set the interface to use for multicast.
	 *
	 * @param iface the {@link NetworkInterface} to use for multicast.
	 * @return {@literal this}
	 */
	public ServerOptions multicastInterface(NetworkInterface iface) {
		this.multicastInterface = iface;
		return this;
	}

	/**
	 * Return the multicast {@link NetworkInterface}
	 *
	 * @return the multicast {@link NetworkInterface}
	 */
	public NetworkInterface multicastInterface() {
		return this.multicastInterface;
	}

	/**
	 * Returns the configured version family for the socket.
	 *
	 * @return the configured version family for the socket
	 */
	public ProtocolFamily protocolFamily() {
		return protocolFamily;
	}

	/**
	 * Configures the version family for the socket.
	 *
	 * @param protocolFamily the version family for the socket, or null for the system default family
	 * @return {@code this}
	 */
	public ServerOptions protocolFamily(ProtocolFamily protocolFamily) {
		this.protocolFamily = protocolFamily;
		return this;
	}

	/**
	 * Returns a boolean indicating whether or not {@code SO_REUSEADDR} is enabled
	 *
	 * @return {@code true} if {@code SO_REUSEADDR} is enabled, {@code false} if it is not
	 */
	public boolean reuseAddr() {
		return reuseAddr;
	}

	/**
	 * Enables or disables {@code SO_REUSEADDR}.
	 *
	 * @param reuseAddr {@code true} to enable {@code SO_REUSEADDR}, {@code false} to disable it
	 * @return {@code this}
	 */
	public ServerOptions reuseAddr(boolean reuseAddr) {
		this.reuseAddr = reuseAddr;
		return this;
	}

	/**
	 * Enable SSL service with a self-signed certificate
	 *
	 * @return {@code this}
	 */
	public ServerOptions sslSelfSigned() {
		SelfSignedCertificate ssc;
		try {
			ssc = new SelfSignedCertificate();
		}
		catch (CertificateException e) {
			throw Exceptions.bubble(e);
		}
		return ssl(SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()));
	}

	/**
	 * @return Immutable {@link ServerOptions}
	 */
	public ServerOptions toImmutable(){
		return new ImmutableServerOptions(this);
	}

	final static class ImmutableServerOptions extends ServerOptions {

		public ImmutableServerOptions(ServerOptions options) {
			super(options);
		}

		@Override
		public ServerOptions afterChannelInit(Consumer<? super Channel> afterChannelInit) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions backlog(int backlog) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions daemon(boolean daemon) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions eventLoopSelector(Supplier<? extends EventLoopSelector> eventLoopGroup) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions keepAlive(boolean keepAlive) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions linger(int linger) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions listen(int port) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions listen(String host, int port) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions listen(InetSocketAddress listenAddress) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions managed(boolean managed) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions multicastInterface(NetworkInterface iface) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions onChannelInit(Predicate<? super Channel> onChannelInit) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions preferNative(boolean preferNative) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions protocolFamily(ProtocolFamily protocolFamily) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions rcvbuf(int rcvbuf) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions reuseAddr(boolean reuseAddr) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions sndbuf(int sndbuf) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions ssl(SslContextBuilder sslOptions) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions sslConfigurer(Consumer<? super SslContextBuilder> consumer) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions sslHandshakeTimeoutMillis(long sslHandshakeTimeoutMillis) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions sslSelfSigned() {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions tcpNoDelay(boolean tcpNoDelay) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions timeoutMillis(long timeout) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ServerOptions toImmutable() {
			return this;
		}
	}

}
