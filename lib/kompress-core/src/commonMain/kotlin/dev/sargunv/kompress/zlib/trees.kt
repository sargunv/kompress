@file:Suppress("FunctionName", "LocalVariableName")

package dev.sargunv.kompress.zlib

/*
 *  ALGORITHM
 *
 *      The "deflation" process uses several Huffman trees. The more
 *      common source values are represented by shorter bit sequences.
 *
 *      Each code tree is stored in a compressed form which is itself
 * a Huffman encoding of the lengths of all the code strings (in
 * ascending order by source values).  The actual code strings are
 * reconstructed from the lengths in the inflate process, as described
 * in the deflate specification.
 *
 *  REFERENCES
 *
 *      Deutsch, L.P.,"'Deflate' Compressed Data Format Specification".
 *      Available in ftp.uu.net:/pub/archiving/zip/doc/deflate-1.1.doc
 *
 *      Storer, James A.
 *          Data Compression:  Methods and Theory, pp. 49-50.
 *          Computer Science Press, 1988.  ISBN 0-7167-8156-5.
 *
 *      Sedgewick, R.
 *          Algorithms, p290.
 *          Addison-Wesley, 1983. ISBN 0-201-06672-6.
 */

/** Initialize the tree data structures for a new zlib stream. */
internal fun DeflateState.tr_init(buf: UByteArray, stored_len: UInt, last: Int) {}

/** Send a stored block */
internal fun DeflateState.tr_stored_block() {}

/**
 * Send one empty static block to give enough lookahead for inflate. This takes 10 bits, of which 7
 * may remain in the bit buffer.
 */
internal fun DeflateState.tr_align() {}

/**
 * Determine the best encoding for the current block: dynamic trees, static trees or store, and
 * write out the encoded block.
 */
internal fun DeflateState.tr_flush_block(buf: UByteArray, stored_len: UInt, last: Int) {}

/**
 * Save the match info and tally the frequency counts. Return true if the current block must be
 * flushed.
 */
internal fun DeflateState.tr_tally(dist: UInt, lc: UInt) {}
