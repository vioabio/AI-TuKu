package com.vio.aitukuviobe.shared.websocket.model;

import com.vio.aitukuviobe.interfaces.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑响应消息（后端 → 前端）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {
    /** 消息类型：INFO / ERROR / ENTER_EDIT / EXIT_EDIT / EDIT_ACTION */
    private String type;
    /** 信息 */
    private String message;
    /** 执行的编辑动作 */
    private String editAction;
    /** 用户信息 */
    private UserVO user;
}
