package interceptor;

import io.grpc.*;

/***
 * 実装参考: https://stackoverflow.com/questions/57370763/how-to-pass-data-from-grpc-rpc-call-to-server-interceptor-in-java
 * インターセプターの処理の流れについては右サイトを参考：https://engineering.kabu.com/entry/2021/03/31/162401
 */
public class getHeadersInterceptor implements ServerInterceptor {

    // メタデータを保持する入れ物
    public static final Context.Key<Metadata> TRAILER_HOLDER_KEY = Context.key("trailerHolder");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call, final Metadata headers, final ServerCallHandler<ReqT, RespT> next) {
        final TrailerCall<ReqT, RespT> call2 = new TrailerCall<>(call);
        // メタデータをコンテキストに格納
        final Context context = Context.current().withValue(TRAILER_HOLDER_KEY, headers);
        final Context previousContext = context.attach();
        try {
            return new TrailerListener<>(next.startCall(call2, headers), context);
        } finally {
            context.detach(previousContext);
        }
    }

    private class TrailerCall<ReqT, RespT> extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {
        public TrailerCall(final ServerCall<ReqT, RespT> delegate) {
            super(delegate);
        }
        /**
         * ここには、TrailerListener()の処理が終わった後にさせる処理を定義する
         */
        @Override
        public void close(final Status status, final Metadata trailers) {
            trailers.merge(TRAILER_HOLDER_KEY.get());
            super.close(status, trailers);
        }
    }
    private class TrailerListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private final Context context;

        public TrailerListener(final ServerCall.Listener<ReqT> delegate, final Context context) {
            super(delegate);
            this.context = context;
        }
        @Override
        public void onMessage(final ReqT message) {
            final Context previous = this.context.attach();
            try {
                super.onMessage(message);
            } finally {
                this.context.detach(previous);
            }
        }
        @Override
        public void onHalfClose() {
            final Context previous = this.context.attach();
            try {
                super.onHalfClose();
            }finally {
                this.context.detach(previous);
            }
        }
        @Override
        public void onCancel(){
            final Context previous = this.context.attach();
            try {
                super.onCancel();
            }finally {
                this.context.detach(previous);
            }
        }
        @Override
        public void onComplete() {
            final Context previous = this.context.attach();
            try {
                super.onComplete();
            } finally {
                this.context.detach(previous);
            }
        }
        @Override
        public void onReady() {
            final Context previous = this.context.attach();
            try {
                super.onReady();
            } finally {
                this.context.detach(previous);
            }
        }
    }
}


