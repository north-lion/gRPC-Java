import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import interceptor.getHeadersInterceptor;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import io.grpc.tool.checktypespec.CheckReply;
import io.grpc.tool.checktypespec.CheckRequest;
import io.grpc.tool.checktypespec.CheckTypeSpecGrpc;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static interceptor.getHeadersInterceptor.TRAILER_HOLDER_KEY;

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
            try {
                Metadata metadata = TRAILER_HOLDER_KEY.get();
                String testId = metadata.get(Metadata.Key.of("x-test-id", Metadata.ASCII_STRING_MARSHALLER));
                if (testId == null) {
                    System.out.println("----------------------WARN testID is not defined----------------------");
                    testId = "";
                }
                System.out.println("testId:" + testId);
                // 受信メッセージ保存
                String resourcePath = "src/output/";
                File file = new File(resourcePath + "_include_def_val_print_" +  testId);
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                try {
                    bw.write(JsonFormat.printer().includingDefaultValueFields().print(req));
                    bw.flush();
                } finally {
                    bw.close();
                }
                file = new File(resourcePath + "_print_" + testId);
                bw = new BufferedWriter(new FileWriter(file));
                try {
                    bw.write(JsonFormat.printer().print(req));
                    bw.flush();
                } finally {
                    bw.close();
                }
                // メッセージ解析＆結果返却
                String tmp = analyseMessage(req).toString();
                CheckReply reply = CheckReply.newBuilder().setMessage(tmp).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
                System.out.println(System.lineSeparator());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static JSONObject analyseMessage(Message request) throws Exception {
        JSONObject result = new JSONObject();
        System.out.println("-----------START writeMessage-----------");
        for (Descriptors.FieldDescriptor fds : request.getDescriptorForType().getFields()) {
            if (fds.isRepeated()) {
                // noop
            } else if (request.hasField(fds)) { // message型に値が入っている場合
                // xx_Specメッセージ判定
                switch (fds.getName()) {
                    case "scalar_spec":
                        CheckRequest.ScalarSpec scalerSpec = (CheckRequest.ScalarSpec) request.getField(fds);
                        result.put("scalar_spec", fieldDespriptor(scalerSpec));
                        break;
                    case "optional_spec":
                        CheckRequest.OptionalSpec optionalSpec = (CheckRequest.OptionalSpec) request.getField(fds);
                        result.put("optional_spec", fieldDespriptor(optionalSpec));
                        break;
                    case "wrapper_spec":
                        CheckRequest.WrapperSpec wrapperSpec = (CheckRequest.WrapperSpec) request.getField(fds);
                        // wrappers.protoは内部実装としてはメッセージ型であるため、取得方法がScaler型、Enum型とは異なる
                        result.put("wrapper_spec", WrappersfieldDespriptor(wrapperSpec));
                        break;
                    case "oneof_spec":
                        CheckRequest.OneofSpec oneofSpec = (CheckRequest.OneofSpec) request.getField(fds);
                        result.put("oneof_spec", fieldDespriptor(oneofSpec));
                        break;
                    case "repeated_spec":
                        CheckRequest.RepeatedSpec repeatedSpec = (CheckRequest.RepeatedSpec) request.getField(fds);
                        result.put("repeated_spec", fieldDespriptor(repeatedSpec));
                        break;
                }
            }
        }
        System.out.println("-----------END writeMessage-----------");
        return result;
    }

    private static JSONObject fieldDespriptor(Message message) throws Exception {
        JSONObject result = new JSONObject();
        for (Descriptors.FieldDescriptor fds : message.getDescriptorForType().getFields()) {
            System.out.println("fds:" + fds.toString());
            if (fds.isRepeated()) { // repeatedのフィールドにhasFieldを実行すると、UnsupportedOperationExceptionとなる
                // Repeatedなフィールドの要素数が0でない場合のみ、格納する
                int counter = message.getRepeatedFieldCount(fds);
                if(counter != 0) {
                    List<Object> list = new ArrayList<>();
                    for (int i =0; i < counter; i++) {
                        if (Descriptors.FieldDescriptor.Type.BYTES.equals(fds.getType())) {
                            ByteString fieldValue = autoCast(message.getRepeatedField(fds,i));
                            list.add(fieldValue.toStringUtf8());
                        } else if (Descriptors.FieldDescriptor.Type.MESSAGE.equals(fds.getType())){
                            CheckRequest.Message fieldValue = autoCast(message.getRepeatedField(fds,i));
                            list.add(new JSONObject(JsonFormat.printer().print(fieldValue)));
                        } else {
                            list.add(message.getRepeatedField(fds, i));
                        }
                    }
                    result.put(fds.getName(), list.toString());
                }

//                if (fds.isMapField()) {
//                    /**
//                     ---- equal ----
//                     Map<Object, Object> map = autoCast(message.getField(fds));
//                     Object fieldValue = map.toString();
//                     result.put(fds.getName(), fieldValue);
//                     */
//                } else {
//                    /**
//                     ---- equal ----
//                     List<Object> list = autoCast(message.getField(fds));
//                     Object fieldValue = list.toString();
//                     result.put(fds.getName(), fieldValue);
//                     */
//                }
            } else if (message.hasField(fds)) { // trueとならない場合は、そのフィールドに値がセットされていない扱いと同様
                System.out.println(fds.getName() + " has Field");
                // bytes型の場合は、contents値のみ取り出す
                if (Descriptors.FieldDescriptor.Type.BYTES.equals(fds.getType())) {
                    ByteString fieldValue = autoCast(message.getField(fds));
                    result.put(fds.getName(), fieldValue.toStringUtf8());
                } else if (Descriptors.FieldDescriptor.Type.MESSAGE.equals(fds.getType())){
                    // message型の場合、自動生成されたgetメソッドを使用して値を取り出す
                    //   result.put(fds.getName(), fieldValue.getStr());

                    /**
                     * 上記方法もしくは、message.getField(fds)で値を取得すると、結果は空文字("")になる
                     * メッセージの場合の値はJsonFormat.Printer()でJSON文字列を出力し、JSONObjectに変換したものをセットする
                     * 意図：Javaで受けとった値との整合性が取れるように調整
                     */
                    CheckRequest.Message fieldValue = autoCast(message.getField(fds));
                    result.put(fds.getName(), new JSONObject(JsonFormat.printer().print(fieldValue)));
                } else {
                    Object fieldValue = message.getField(fds);
                    result.put(fds.getName(), fieldValue);
                }
            } else {
                System.out.println(fds.getName() + " has not Field");
            }
        }
        return result;
    }
    private static JSONObject WrappersfieldDespriptor(CheckRequest.WrapperSpec message) {
        JSONObject result = new JSONObject();
        for (Descriptors.FieldDescriptor fds : message.getDescriptorForType().getFields()) {
//            System.out.println("fds:" + fds.toString());
            /**  wrappers.protoは内部実装としてはメッセージ型であるため、repeatedの仕様は自前のメッセージ型で検証
             *   Ref: https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/wrappers.proto
             */
            if (message.hasField(fds)) { // trueとならない場合は、そのフィールドに値がセットされていない扱いと同様
                // bytes型の場合は、contents値のみ取り出す
                switch(fds.getName()) {
                    case "str":
                        result.put("str", message.getStr().getValue());
                        break;
                    case "int32":
                        result.put("int32", message.getInt32().getValue());
                        break;
                    case "bool":
                        result.put("bool", message.getBool().getValue());
                        break;
                    case "bytes":
                        result.put("bytes", message.getBytes().getValue().toStringUtf8());
                        break;
                    default:
                        System.out.println("No Difined Name Value!");
                }
            }
        }
        return result;
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
}