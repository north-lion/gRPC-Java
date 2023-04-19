import com.google.protobuf.Message;
import io.grpc.*;


public class getHeadersInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(call, headers)) {
            Message request;

            @Override
            public void onMessage(final ReqT message) {
                if (request != null) {
                    throw new AssertionError("ServerCallListener may be reused !");
                }
                if (message == null) {
                    throw new AssertionError("Received null message !");
                }
                request = (Message) message;

                saveHeader();

                super.onMessage(message);
            }
            @Override
            public void onComplete() {
                super.onComplete();

                dropHeader();
            }
            @Override
            public void onCancel() {
                super.onCancel();

                dropHeader();
            }

            private void saveHeader() {
                if (request == null) {
                    throw new IllegalStateException("Reuqest is null !");
                }
                HeaderCarrier.setMetadata(headers);
            }

            private void dropHeader() {
                if (request == null) {
                    return;
                }
                HeaderCarrier.removeMetadata();;
            }
        };
    }
}


