package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ServerProperties;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class CreateDashLogic {

    private static final Logger log = LogManager.getLogger(CreateDashLogic.class);

    private final int DASH_MAX_LIMIT;
    private final int DASH_MAX_SIZE;

    public CreateDashLogic(ServerProperties props) {
        this.DASH_MAX_LIMIT = props.getIntProperty("user.dashboard.max.limit");
        this.DASH_MAX_SIZE = props.getIntProperty("user.profile.max.size") * 1024;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String dashString = message.body;

        //expecting message with 2 parts
        if (dashString == null || dashString.equals("")) {
            throw new IllegalCommandException("Income create dash message is empty.", message.id);
        }

        if (dashString.length() > DASH_MAX_SIZE) {
            throw new NotAllowedException("User dashboard is larger then limit.", message.id);
        }

        log.debug("Trying to parse user newDash : {}", dashString);
        DashBoard newDash = JsonParser.parseDashboard(dashString, message.id);

        log.info("Creating new dashboard.");

        if (user.profile.dashBoards.length > DASH_MAX_LIMIT) {
            throw new NotAllowedException("Dashboards limit reached.", message.id);
        }

        for (DashBoard dashBoard : user.profile.dashBoards) {
            if (dashBoard.id == newDash.id) {
                throw new NotAllowedException("Dashboard already exists.", message.id);
            }
        }

        user.profile.dashBoards = ArrayUtil.add(user.profile.dashBoards, newDash);
        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(produce(message.id, OK));
    }

}