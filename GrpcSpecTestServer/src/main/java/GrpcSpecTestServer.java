import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import io.grpc.tool.checktypespec.CheckReply;
import io.grpc.tool.checktypespec.CheckRequest;
import io.grpc.tool.checktypespec.CheckTypeSpecGrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GrpcSpecTestServer {

    public static void main(String[] args) throws Exception {

        Server server = ServerBuilder.forPort(50051)
                .addService(ServerInterceptors.intercept(new CheckTypeSpecGrpcImpl(), new getHeadersInterceptor()))
                .addService(ProtoReflectionService.newInstance())
                .build();

        System.out.println("Starting server...");
        server.start();
        System.out.println("Server started!");
        server.awaitTermination();;
    }

    static class CheckTypeSpecGrpcImpl extends CheckTypeSpecGrpc.CheckTypeSpecImplBase {
        @Override
        public void uploadCheckResult(CheckRequest req, StreamObserver<CheckReply> responseObserver) {
            System.out.println("----------------------Received Request----------------------");
            Metadata metadata = HeaderCarrier.getMetadata();
            String testId = metadata.get(Metadata.Key.of("x-test-id", Metadata.ASCII_STRING_MARSHALLER));
            if (testId == null) {
                System.out.println("----------------------WARN testID is not defined----------------------");
                testId = "";
            }
            Map<String, Object> result = new HashMap<>();
            result.put(testId, writeMessage(req));

            CheckReply reply = CheckReply.newBuilder().setMessage(result.toString()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
            System.out.println(System.lineSeparator());
        }
    }

    private static Map<String, Object> writeMessage(Message request) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            System.out.println("-----------START writeMessage-----------");
            for (Descriptors.FieldDescriptor fds : request.getDescriptorForType().getFields()) {
                if (fds.isRepeated()) {
                    // noop
                } else if (request.hasField(fds)) { // message型に値が入っている場合
                    // メッセージ判定
                    switch(fds.getName()) {
                        case "scalar_spec":
                            CheckRequest.ScalarSpec scalerSpec = (CheckRequest.ScalarSpec)request.getField(fds);
                            resultMap.put("scalar_spec" ,fieldDespriptor(scalerSpec));
                            break;
                        case "optional_spec":
                            CheckRequest.OptionalSpec optionalSpec = (CheckRequest.OptionalSpec)request.getField(fds);
                            resultMap.put("optional_spec" ,fieldDespriptor(optionalSpec));
                            break;
                        case "wrapper_spec":
                            CheckRequest.WrapperSpec wrapperSpec = (CheckRequest.WrapperSpec)request.getField(fds);
                            resultMap.put("wrapper_spec" ,fieldDespriptor(wrapperSpec));
                            break;
                        case "oneof_spec":
                            CheckRequest.OneofSpec oneofSpec = (CheckRequest.OneofSpec)request.getField(fds);
                            resultMap.put("oneof_spec" ,fieldDespriptor(oneofSpec));
                            break;
                        case "repeated_spec":
                            CheckRequest.RepeatedSpec repeatedSpec = (CheckRequest.RepeatedSpec)request.getField(fds);
                            resultMap.put("repeated_spec" ,fieldDespriptor(repeatedSpec));
                            break;
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("-----------END writeMessage-----------");
        return resultMap;
    }

    /**
     * <p>[概 要] 戻り値の型に合わせてキャスト</p>
     * <p>[詳 細] </p>
     * <p>[備 考] </p>
     */
    @SuppressWarnings("unchecked")
    public static <T> T autoCast(Object obj) {
        T castObj = (T) obj;
        return castObj;
    }

    private static Map<String, Object> fieldDespriptor(Message message){
        Map<String, Object> kvMap = new HashMap<>();
        for (Descriptors.FieldDescriptor fds : message.getDescriptorForType().getFields()) {
            if (fds.isRepeated()) {
                if (fds.isMapField()) {
                    Map<Object, Object> map = autoCast(message.getField(fds));
                    Object fieldValue = map.toString();
                    kvMap.put(fds.getName(), fieldValue);
                } else {
                    List<Object> list = autoCast(message.getField(fds));
                    Object fieldValue = list.toString();
                    kvMap.put(fds.getName(), fieldValue);
                }
            } else if (message.hasField(fds)) { // hasFieldがtrueにならない場合はval追加無し
                Object fieldValue = message.getField(fds);
                kvMap.put(fds.getName(), fieldValue);
            }
        }
        return kvMap;
    }
}