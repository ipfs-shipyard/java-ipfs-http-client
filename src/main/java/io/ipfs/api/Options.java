package io.ipfs.api;

/**
 * Constant values used to set operation arguments.
 *
 * See https://docs.ipfs.io/reference/http/api/#api-v0-file-ls
 */
public interface Options {

    /** quiet [bool]: Write minimal output. Required: no. **/
    public String QUIET = "quiet";
    /**  quieter [bool]: Write only final hash. Required: no. **/
    public String QUIETER = "quieter";
    /**  silent [bool]: Write no output. Required: no. **/
    public String SILENT = "silent";
    /**  progress [bool]: Stream progress data. Required: no. **/
    public String PROGRESS = "progress";
    /**  trickle [bool]: Use trickle-dag format for dag generation. Required: no. **/
    public String TRICKLE = "trickle";
    /**  only-hash [bool]: Only chunk and hash - do not write to disk. Required: no. **/
    public String ONLY_HASH = "only-hash";
    /**  wrap-with-directory [bool]: Wrap files with a directory object. Required: no. **/
    public String WRAP_WITH_DIRECTORY = "wrap-with-directory";
    /**  chunker [string]: Chunking algorithm, size-[bytes], rabin-[min]-[avg]-[max] or buzhash. Default: size-262144. Required: no. **/
    public String CHUNKER = "chunker";
    /**  pin [bool]: Pin this object when adding. Default: true. Required: no. **/
    public String PIN = "pin";
    /**  raw-leaves [bool]: Use raw blocks for leaf nodes. (experimental). Required: no. **/
    public String RAW_LEAVES = "raw-leaves";
    /**  nocopy [bool]: Add the file using filestore. Implies raw-leaves. (experimental). Required: no. **/
    public String NOCOPY = "nocopy";
    /**  fscache [bool]: Check the filestore for pre-existing blocks. (experimental). Required: no. **/
    public String FSCACHE = "fscache";
    /**  cid-version [int]: CID version. Defaults to 0 unless an option that depends on CIDv1 is passed. (experimental). Required: no. **/
    public String CID_VERSION = "cid-version";
    /**  hash [string]: Hash function to use. Implies CIDv1 if not sha2-256. (experimental). Default: sha2-256. Required: no. **/
    public String HASH = "hash";
    /**  inline [bool]: Inline small blocks into CIDs. (experimental). Required: no. **/
    public String INLINE = "inline";
    /**  inline-limit [int]: Maximum block size to inline. (experimental). Default: 32. Required: no. **/
    public String INLINE_LIMIT = "inline-limit";

}
