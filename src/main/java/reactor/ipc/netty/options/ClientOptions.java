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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.NetUtil;

/**
 * A client connector builder with low-level connection options including connection pooling and
 * proxy.
 *
 * @author Stephane Maldini
 */
public class ClientOptions extends NettyOptions<ClientOptions> {

	/**
	 * Proxy Type
	 */
	public enum Proxy {
		HTTP, SOCKS4, SOCKS5
	}

	/**
	 * Create a client builder.
	 *
	 * @return a new client options builder
	 */
	public static ClientOptions create() {
		return new ClientOptions();
	}

	/**
	 * Create a client builder to connect on a given host by default. Resolution of the
	 * host will be applied for each client new handler and after proxy if present.
	 * Remote Port will be @{link #DEFAULT_PORT}.
	 *
	 * @param host the default remote host to connect to.
	 *
	 * @return a new client options builder
	 */
	public static ClientOptions to(String host) {
		return to(host, DEFAULT_PORT);
	}

	/**
	 * Create a client builder to connect on a given host and port by default.
	 * Resolution of the
	 * host will be applied for each client new handler and after proxy if present.
	 *
	 * @param host the default remote host to connect to.
	 * @param port the default remote port to connect to.
	 *
	 * @return a new client options builder
	 */
	public static ClientOptions to(String host, int port) {
		return create().connect(host, port);
	}

	/**
	 * Client connection pool options
	 */
	boolean                                                    channelPool;
	long                                                       channelPoolAcquireMillis;
	int                                                        channelPoolSize;
	int                                                        channelPoolAcquireSize;
	Function<? super InetSocketAddress, ? extends ChannelPool> channelPoolResolver;

	/**
	 * Proxy options
	 */
	String                                     proxyUsername;
	Function<? super String, ? extends String> proxyPassword;
	Supplier<? extends InetSocketAddress>      proxyAddress;
	Proxy                                      proxyType;

	Supplier<? extends InetSocketAddress> connectAddress = DEFAULT_ADDRESS;

	ClientOptions() {
	}

	ClientOptions(ClientOptions options){
		super(options);
		this.proxyUsername = options.proxyUsername;
		this.proxyPassword = options.proxyPassword;
		this.proxyAddress = options.proxyAddress;
		this.proxyType = options.proxyType;
		this.connectAddress = options.connectAddress;
	}

	/**
	 * The host and port to which this client should connect.
	 *
	 * @param host The host to connect to.
	 * @param port The port to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions connect(@Nonnull String host, int port) {
		return connect(InetSocketAddress.createUnresolved(host, port));
	}

	/**
	 * The address to which this client should connect.
	 *
	 * @param connectAddress The address to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions connect(@Nonnull InetSocketAddress connectAddress) {
		return connect(new InetResolverSupplier(connectAddress, this));
	}

	/**
	 * The address to which this client should connect.
	 *
	 * @param connectAddress The address to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions connect(@Nonnull Supplier<? extends InetSocketAddress> connectAddress) {
		this.connectAddress = connectAddress;
		return this;
	}

	@Override
	public ClientOptions duplicate() {
		return new ClientOptions(this);
	}

	/**
	 * The host and port to which this client should connect.
	 *
	 * @param host The host to connect to.
	 * @param port The port to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions proxy(@Nonnull Proxy type,
			@Nonnull String host,
			int port,
			@Nullable String username,
			@Nullable Function<? super String, ? extends String> password) {
		return proxy(type,
				InetSocketAddress.createUnresolved(host, port),
				username,
				password);
	}

	/**
	 * The host and port to which this client should connect.
	 *
	 * @param host The host to connect to.
	 * @param port The port to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions proxy(@Nonnull Proxy type, @Nonnull String host, int port) {
		return proxy(type, InetSocketAddress.createUnresolved(host, port));
	}

	/**
	 * The address to which this client should connect.
	 *
	 * @param connectAddress The address to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions proxy(@Nonnull Proxy type,
			@Nonnull InetSocketAddress connectAddress) {
		return proxy(type, new InetResolverProxySupplier(connectAddress), null, null);
	}

	/**
	 * The address to which this client should connect.
	 *
	 * @param connectAddress The address to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions proxy(@Nonnull Proxy type,
			@Nonnull InetSocketAddress connectAddress,
			@Nullable String username,
			@Nullable Function<? super String, ? extends String> password) {
		return proxy(type,
				new InetResolverProxySupplier(connectAddress),
				username,
				password);
	}

	/**
	 * The address to which this client should connect.
	 *
	 * @param connectAddress The address to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions proxy(@Nonnull Proxy type,
			@Nonnull Supplier<? extends InetSocketAddress> connectAddress) {
		return proxy(type, connectAddress, null, null);
	}

	/**
	 * The address to which this client should connect.
	 *
	 * @param connectAddress The address to connect to.
	 *
	 * @return {@literal this}
	 */
	public ClientOptions proxy(@Nonnull Proxy type,
			@Nonnull Supplier<? extends InetSocketAddress> connectAddress,
			@Nullable String username,
			@Nullable Function<? super String, ? extends String> password) {
		this.proxyUsername = username;
		this.proxyPassword = password;
		this.proxyAddress = Objects.requireNonNull(connectAddress, "addressSupplier");
		this.proxyType = Objects.requireNonNull(type, "proxyType");
		return this;
	}

	/**
	 * Return the eventual remote host
	 *
	 * @return the eventual remote host
	 */
	public InetSocketAddress remoteAddress() {
		return connectAddress != null ? connectAddress.get() : null;
	}

	/**
	 * @return this {@link ClientOptions}
	 */
	public ClientOptions sslSupport() {
		super.ssl(SslContextBuilder.forClient());
		return this;
	}

	/**
	 * Proxy username if any
	 *
	 * @return a proxy username String
	 */
	public String proxyUsername() {
		return proxyUsername;
	}

	/**
	 * Proxy password selector if any
	 *
	 * @return a proxy password selector
	 */
	public Function<? super String, ? extends String> proxyPassword() {
		return proxyPassword;
	}

	/**
	 * Proxy address supplier if any
	 *
	 * @return a proxy address supplier
	 */
	public Supplier<? extends InetSocketAddress> proxyAddress() {
		return proxyAddress;
	}

	/**
	 * {@link Proxy} category to usePredicate
	 *
	 * @return {@link Proxy} category to use
	 */
	public Proxy proxyType() {
		return proxyType;
	}

	/**
	 * @return immutable {@link ClientOptions}
	 */
	public ClientOptions toImmutable() {
		return new ImmutableClientOptions(this);
	}

	final static class ImmutableClientOptions extends ClientOptions {

		public ImmutableClientOptions(ClientOptions options) {
			super(options);
		}

		@Override
		public ClientOptions toImmutable() {
			return this;
		}

		@Override
		public ClientOptions sslSupport() {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions timeoutMillis(long timeout) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions sslHandshakeTimeoutMillis(long sslHandshakeTimeoutMillis) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions daemon(boolean daemon) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions connect(@Nonnull String host, int port) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions proxy(@Nonnull Proxy type,
				@Nonnull Supplier<? extends InetSocketAddress> connectAddress,
				@Nullable String username,
				@Nullable Function<? super String, ? extends String> password) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions connect(@Nonnull InetSocketAddress connectAddress) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions eventLoopSelector(Supplier<? extends EventLoopSelector> eventLoopGroup) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions keepAlive(boolean keepAlive) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions linger(int linger) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions managed(boolean managed) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions afterChannelInit(Consumer<? super Channel> afterChannelInit) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions connect(@Nonnull Supplier<? extends InetSocketAddress> connectAddress) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions proxy(@Nonnull Proxy type,
				@Nonnull String host,
				int port,
				@Nullable String username,
				@Nullable Function<? super String, ? extends String> password) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions proxy(@Nonnull Proxy type, @Nonnull String host, int port) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions proxy(@Nonnull Proxy type,
				@Nonnull InetSocketAddress connectAddress) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions proxy(@Nonnull Proxy type,
				@Nonnull InetSocketAddress connectAddress,
				@Nullable String username,
				@Nullable Function<? super String, ? extends String> password) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions proxy(@Nonnull Proxy type,
				@Nonnull Supplier<? extends InetSocketAddress> connectAddress) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions preferNative(boolean preferNative) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions rcvbuf(int rcvbuf) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions ssl(SslContextBuilder sslOptions) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions sslConfigurer(Consumer<? super SslContextBuilder> consumer) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions sndbuf(int sndbuf) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions onChannelInit(Predicate<? super Channel> onChannelInit) {
			throw new UnsupportedOperationException("Immutable Options");
		}

		@Override
		public ClientOptions tcpNoDelay(boolean tcpNoDelay) {
			throw new UnsupportedOperationException("Immutable Options");
		}
	}

	static final class InetResolverSupplier implements Supplier<InetSocketAddress> {

		final InetSocketAddress connectAddress;
		final ClientOptions     parent;

		public InetResolverSupplier(InetSocketAddress address, ClientOptions parent) {
			this.connectAddress = address;
			this.parent = parent;
		}

		@Override
		public InetSocketAddress get() {
			return connectAddress.isUnresolved() && parent.proxyType == null ?
					new InetSocketAddress(connectAddress.getHostName(),
							connectAddress.getPort()) : connectAddress;
		}
	}

	static final class InetResolverProxySupplier implements Supplier<InetSocketAddress> {

		final InetSocketAddress connectAddress;

		public InetResolverProxySupplier(InetSocketAddress address) {
			this.connectAddress = address;
		}

		@Override
		public InetSocketAddress get() {
			return connectAddress.isUnresolved() ?
					new InetSocketAddress(connectAddress.getHostName(),
							connectAddress.getPort()) : connectAddress;
		}
	}

	static final InetSocketAddress           LOCALHOST       =
			new InetSocketAddress(NetUtil.LOCALHOST.getHostAddress(), DEFAULT_PORT);
	static final Supplier<InetSocketAddress> DEFAULT_ADDRESS = () -> LOCALHOST;
}
