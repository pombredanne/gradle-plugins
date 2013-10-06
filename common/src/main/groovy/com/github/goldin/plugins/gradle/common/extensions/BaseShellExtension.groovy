package com.github.goldin.plugins.gradle.common.extensions


/**
 * Base class for extension describing plugin generating shell scripts
 */
class BaseShellExtension extends BaseExtension
{
    String        shell        = '/bin/bash'
    List<Closure> transformers = []    // Callbacks to invoke after every shell script is generated
    boolean       verbose      = false // Whether scripts generated should have 'set -x' added (will print out every command executed)

    /**
     * Whether color codes should be removed from command outputs.
     */
    boolean      removeColor   = 'BUILD_NUMBER JENKINS_URL TEAMCITY_VERSION'.split().any{ System.getenv( it ) != null }
    String       removeColorCodes // Internal property
}
