package com.github.goldin.plugins.gradle.play.tasks

import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.ShellHelper
import com.github.goldin.plugins.gradle.play.PlayExtension


/**
 * Base class for all Play plugin tasks.
 */
abstract class PlayBaseTask extends BaseTask<PlayExtension>
{
    @Delegate ShellHelper  shellHelper

    @Override
    Class<PlayExtension> extensionType (){ PlayExtension }


    @Override
    void verifyUpdateExtension ( String description )
    {
        shellHelper = new ShellHelper( this.project, this, this.ext )

        assert ext.playVersion,   "'playVersion' should be defined in $description"
        assert ext.playUrl,       "'playUrl' should be defined in $description"
        assert ext.address,       "'address' should be defined in $description"
        assert ext.conf,          "'conf' should be defined in $description"
        assert ext.port      > 0, "'port' should be positive in $description"
        assert ext.debugPort > 0, "'debugPort' should be positive in $description"
    }
}