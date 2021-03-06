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

package reactor.ipc.netty.http.client;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.channel.NettyOperations;
import reactor.ipc.netty.http.HttpOutbound;

/**
 * An Http Reactive client write contract for outgoing requests. It inherits several
 * accessor related to HTTP flow : headers, params, URI, method, websocket...
 *
 * @author Stephane Maldini
 */
public interface HttpClientRequest extends HttpOutbound {
	/**
	 * add the passed cookie
	 * @return this
	 */
	@Override
	HttpClientRequest addCookie(Cookie cookie);

	@Override
	HttpClientRequest keepAlive(boolean keepAlive);

	@Override
	HttpClientRequest disableChunkedTransfer();

	@Override
	HttpClientRequest addHeader(CharSequence name, CharSequence value);

	@Override
	HttpClientRequest flushEach();

	/**
	 * Enable http status 302 auto-redirect support
	 *
	 * @return {@literal this}
	 */
	HttpClientRequest followRedirect();

	/**
	 * @param name
	 * @param value
	 *
	 * @return
	 */
	HttpClientRequest header(CharSequence name, CharSequence value);

	/**
	 * Return true  if redirected will be followed
	 *
	 * @return true if redirected will be followed
	 */
	boolean isFollowRedirect();

	/**
	 * Return the previous redirections or empty array
	 *
	 * @return the previous redirections or empty array
	 */
	String[] redirectedFrom();

	/**
	 *
	 * @return
	 */
	HttpHeaders requestHeaders();



	/**
	 * Upgrade connection to Websocket
	 * @return a {@link Mono} completing when upgrade is confirmed
	 */
	default Mono<Void> upgradeToWebsocket() {
		return upgradeToWebsocket(uri(), false, NettyOperations.noopHandler());
	}

	/**
	 * Upgrade connection to Websocket with text plain payloads
	 * @return a {@link Mono} completing when upgrade is confirmed
	 */
	default Mono<Void> upgradeToTextWebsocket() {
		return upgradeToWebsocket(uri(), true, NettyOperations.noopHandler());
	}

}
