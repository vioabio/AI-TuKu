package com.vio.aitukuviobe.shared.auth.model;

import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import lombok.Data;

/**
 * 空间用户权限上下文
 */
@Data
public class SpaceUserAuthContext {
    private Long id;
    private Long pictureId;
    private Long spaceId;
    private Long spaceUserId;
    private Picture picture;
    private Space space;
    private SpaceUser spaceUser;
}
