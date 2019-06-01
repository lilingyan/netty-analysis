package com.lilingyan.netty.channel;

import com.lilingyan.netty.util.internal.TypeParameterMatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public abstract class SimpleChannelInboundHandler<I> extends ChannelInboundHandlerAdapter {

    /**
     * 入参类型校验器
     */
    private final TypeParameterMatcher matcher;
    /**
     * 是否自动释放
     */
    private final boolean autoRelease;

    protected SimpleChannelInboundHandler() {
        this(true);
    }

    protected SimpleChannelInboundHandler(boolean autoRelease) {
        /**
         * 校验类型是<I>
         *     如果是<I>的实现或子类实现才使用这个handler做处理
         *     如果是object，则全部匹配处理
         *     具体看TypeParameterMatcher实现
         */
        matcher = TypeParameterMatcher.find(this, SimpleChannelInboundHandler.class, "I");
        this.autoRelease = autoRelease;
    }

    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType) {
        this(inboundMessageType, true);
    }

    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType, boolean autoRelease) {
        matcher = TypeParameterMatcher.get(inboundMessageType);
        this.autoRelease = autoRelease;
    }

    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean release = true;
        try {
            /**
             * 先判断，是否是当前handler需要处理的数据
             */
            if (acceptInboundMessage(msg)) {
                //强转成泛型
                @SuppressWarnings("unchecked")
                I imsg = (I) msg;
                /**
                 * 具体的实现类做处理
                 *
                 * 数据流向有实现类控制
                 */
                channelRead0(ctx, imsg);
            } else {
                release = false;
                /**
                 * 如果不是当前handler需要处理的数据
                 * 向后抛
                 */
                ctx.fireChannelRead(msg);
            }
        } finally {
            if (autoRelease && release) {
                //自动释放
                ReferenceCountUtil.release(msg);
            }
        }
    }

    protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;
}
