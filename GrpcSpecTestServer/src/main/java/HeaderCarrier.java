import io.grpc.Metadata;

public class HeaderCarrier {

    private static ThreadLocal<Metadata> metadata = new ThreadLocal<Metadata>() {
        @Override
        protected Metadata initialValue() {
            return new Metadata();
        }
    };

    /**
     * 現行スレッドのMetadataオブジェクトを返す
     *
     * @return
     */
    public static Metadata getMetadata() {
        return metadata.get();
    }

    /**
     * 現行スレッドのMetadataオブジェクトに値を設定する
     *
     * @param argsMetadata
     */
    public static void setMetadata(Metadata argsMetadata) {
        metadata.set(argsMetadata);
    }

    /**
     * 現行スレッドのMetadataオブジェクトを削除する
     *
     */
    public static void removeMetadata() {
        metadata.remove();
    }


}
