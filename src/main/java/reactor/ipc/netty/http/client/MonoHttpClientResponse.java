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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

/**
 * @author Stephane Maldini
 */
final class MonoHttpClientResponse extends Mono<HttpClientResponse> {

	final HttpClient                                                     client;
	final URI                                                            startURI;
	final HttpMethod                                                     method;
	final Function<? super HttpClientRequest, ? extends Publisher<Void>> handler;
	final boolean                                                        secure;

	static final AsciiString ALL = new AsciiString("*/*");

	MonoHttpClientResponse(HttpClient client, URI startURI,
			HttpMethod method,
			Function<? super HttpClientRequest, ? extends Publisher<Void>> handler) {
		this.client = client;
		this.startURI = startURI;
		this.method = method;
		this.handler = handler;
		this.secure = startURI.getScheme()
		                      .equalsIgnoreCase(HttpClient.HTTPS_SCHEME) || startURI.getScheme()
		                                                                            .equalsIgnoreCase(
				                                                                            HttpClient.WSS_SCHEME);

	}

	@Override
	@SuppressWarnings("unchecked")
	public void subscribe(final Subscriber<? super HttpClientResponse> subscriber) {
		ReconnectableBridge bridge = new ReconnectableBridge(secure);
		bridge.activeURI = startURI;

		Mono.defer(() -> client.newHandler(bridge.activeURI, bridge, (in, out) -> {
			try {
				URI uri = bridge.activeURI;
				HttpClientOperations ch = (HttpClientOperations) in;
				ch.getNettyRequest()
				  .setUri(uri.getPath() + (uri.getQuery() == null ? "" :
						  "?" + uri.getRawQuery()))
				  .setMethod(method)
				  .setProtocolVersion(HttpVersion.HTTP_1_1)
				  .headers()
				  .add(HttpHeaderNames.HOST, uri.getHost())
				  .add(HttpHeaderNames.ACCEPT, ALL);

				if (method == HttpMethod.GET || method == HttpMethod.HEAD) {
					ch.disableChunkedTransfer();
				}

				if (handler != null) {
					return handler.apply(out);
				}
				else {
					HttpUtil.setTransferEncodingChunked(ch.nettyRequest, false);
					return ch.sendHeaders();
				}
			}
			catch (Throwable t) {
				return Mono.error(t);
			}
		}))
		    .retry(bridge)
		    .cast(HttpClientResponse.class)
		    .subscribe(subscriber);
	}

	static final class ReconnectableBridge
			implements Predicate<Throwable>, Consumer<Channel> {

		final boolean isSecure;

		volatile URI      activeURI;
		volatile String[] redirectedFrom;

		public ReconnectableBridge(boolean isSecure) {
			this.isSecure = isSecure;
		}

		void redirect(String to) {
			String[] redirectedFrom = this.redirectedFrom;
			URI from = activeURI;
			try {
				activeURI = new URI(to);
			}
			catch (URISyntaxException e) {
				throw Exceptions.propagate(e);
			}
			if (redirectedFrom == null) {
				this.redirectedFrom = new String[]{from.toString()};
			}
			else {
				String[] newRedirectedFrom = new String[redirectedFrom.length + 1];
				System.arraycopy(redirectedFrom,
						0,
						newRedirectedFrom,
						0,
						redirectedFrom.length);
				newRedirectedFrom[redirectedFrom.length] = from.toString();
				this.redirectedFrom = newRedirectedFrom;
			}
		}

		@Override
		public void accept(Channel channel) {
			String[] redirectedFrom = this.redirectedFrom;
			if (redirectedFrom != null) {
				channel.attr(HttpClientOperations.REDIRECT_ATTR_KEY)
				       .set(redirectedFrom);
			}
		}

		@Override
		public boolean test(Throwable throwable) {
			if (throwable instanceof RedirectClientException) {
				RedirectClientException re = (RedirectClientException) throwable;
				redirect(re.location);
				return true;
			}
			return false;
		}
	}
}

