package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.node.ConfigHelper
import com.github.goldin.plugins.gradle.node.SetupCacheHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.file.CopySpec
import java.util.regex.Pattern


/**
 * Setup Node.js environment.
 */
class SetupTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        verifyGitAvailable()
        cleanWorkspace()
        ext.configsResult = updateConfigs()
        makeReplacements()
        if ( ext.npmLocalCache || ext.npmRemoteCache ){ restoreNodeModulesFromCache() }
        runSetupScript()
        if ( ext.npmLocalCache || ext.npmRemoteCache ){ createNodeModulesCache() }
    }


    private cleanWorkspace()
    {
        if ( ext.cleanWorkspace )
        {
            for ( command in ext.cleanWorkspaceCommands )
            {
                final commandSplit = command.trim().tokenize()
                exec( commandSplit.head(), commandSplit.tail(), projectDir )
            }
        }
    }


    @SuppressWarnings([ 'JavaStylePropertiesInvocation', 'GroovyGetterCallCanBePropertyAccess' ])
    @Ensures({ result != null })
    private List<Map<String, ?>> updateConfigs()
    {
        final configs      = []
        final configHelper = new ConfigHelper( ext, this )

        for ( configMap in ( ext.configs ?: [] ))
        {
            configMap.each {
                String configPath, Object configValue ->

                final configFile  = project.file( configPath )
                final isValueFile = configValue instanceof File
                final isValueMap  = configValue instanceof Map

                assert ( isValueFile || isValueMap ), \
                       "Config value for [$configFile.canonicalPath] is of type [${ configValue?.getClass()?.name }], " +
                       "should be of type [$File.name] or [$Map.name]"

                log{ "Merging JSON config [$configFile.canonicalPath] with " +
                     ( isValueFile ? "[${ (( File ) configValue ).canonicalPath }]" : configValue.toString() ) }

                configs << configHelper.updateConfigFile (
                    configFile,
                    ( isValueFile ? configHelper.readConfigFile(( File ) configValue ) : ( Map ) configValue ))
            }
        }

        if ( ext.configs && ext.printConfigs ) { printConfigs() }
        configs
    }


    @Requires({ ext.configs })
    private void printConfigs ()
    {
        for ( configFile in ext.configs*.keySet().flatten()*.toString().toSet().collect{ file( it )})
        {
            final configContent = ( ext.printConfigsMask ?: [] ).inject( configFile.getText( 'UTF-8' )){
                String content, String maskProperty ->
                content.replaceAll( ~/("\Q$maskProperty\E"\s*:\s*)([^,\r\n\}]+)/ ){ "${ it[ 1 ]}\"...\"" }
            }

            log{ """
                 |file:$configFile.canonicalPath
                 |$LOG_DELIMITER
                 |$configContent
                 |$LOG_DELIMITER
                 """.stripMargin().toString().trim() }
        }
    }


    @SuppressWarnings([ 'BuilderMethodWithSideEffects' ])
    private void makeReplacements()
    {
        if ( ! ext.replaces ){ return }

        for ( replacesMap in ext.replaces )
        {
            replacesMap.each {
                String replacePath, Map replaces ->

                final replaceFile    = file( replacePath )

                log{ "Updating [$replaceFile.canonicalPath] using replacements Map $replaces" }

                final String content = replaces.inject( replaceFile.getText( 'UTF-8' )) {
                    String content, String replacePattern, Object replaceContent ->

                    assert content && replacePattern && ( replaceContent != null ), \
                           'Content to replace, matching pattern and replacement content should be defined'

                    if ( replacePattern.with { startsWith( '/' ) && endsWith( '/' ) })
                    {
                        final  pattern = replacePattern[ 1 .. -2 ]
                        assert pattern, 'Empty regex replace patterns are not allowed'
                        content.replaceAll( Pattern.compile( pattern ), replaceContent.toString())
                    }
                    else
                    {
                        content.replace( replacePattern, replaceContent.toString())
                    }
                }

                assert content, 'Resulting content after all replaces are made is empty'
                write( replaceFile, content )
            }
        }
    }


    @Requires({ ext.npmLocalCache || ext.npmRemoteCache })
    private void restoreNodeModulesFromCache ()
    {
        final nodeModules = new File ( projectDir, 'node_modules' )
        if ( nodeModules.directory ) { return }

        final npmCache = new SetupCacheHelper( this ).localArchive()
        if ( ! npmCache?.file ) { return }

        logger.info( "Unpacking 'npm install' cache [$npmCache.canonicalPath] to [$projectDir.canonicalPath]" )

        exec( 'tar', [ '-xzf', npmCache.canonicalPath, '-C', projectDir.canonicalPath ], projectDir )
    }


    @SuppressWarnings([ 'GroovyMultipleReturnPointsPerMethod' ])
    @Requires({ ext.npmLocalCache || ext.npmRemoteCache })
    private void createNodeModulesCache ()
    {
        final nodeModules = new File ( projectDir, 'node_modules' )
        if ( ! ( nodeModules.directory && ext.npmCleanInstall )) { return }

        final npmCache = new SetupCacheHelper( this ).localArchive()
        if (( npmCache == null ) || npmCache.file ){ return }

        logger.info( "Packing 'npm install' result [$nodeModules.canonicalPath] to [$npmCache.canonicalPath]" )

        npmCache.parentFile.with { File f -> assert ( f.directory || f.mkdirs()), "Failed to mkdir [$f.canonicalPath]" }
        final tempFile = project.file( npmCache.name )

        project.copy { CopySpec cs -> cs.from( PACKAGE_JSON ).into( nodeModules ) }
        final packageJson        = new File( nodeModules, PACKAGE_JSON )
        final packageJsonMap     = jsonToMap( packageJson.getText( 'UTF-8' ), packageJson )
        packageJsonMap.cacheData = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
        packageJsonMap.timestamp = dateFormatter.format( new Date())
        packageJsonMap.host      = "${ hostname() }/${ InetAddress.localHost.hostAddress }".toString()
        packageJsonMap.project   = project.toString()
        packageJsonMap.directory = projectDir.canonicalPath
        packageJsonMap.origin    = gitExec( 'remote -v', projectDir ).readLines().find { it.startsWith( 'origin' )}.split()[ 1 ]
        packageJsonMap.sha       = gitExec( 'log -1 --format=format:%H', projectDir )
        packageJson.setText( objectToJson( packageJsonMap ), 'UTF-8' )

        exec( 'tar', [ '-czf', tempFile.canonicalPath, nodeModules.name ], projectDir )
        while( ! tempFile.renameTo( npmCache )){ sleep( 1000 ) }
    }


    private void runSetupScript()
    {
        final setupScript = getResourceText( 'setup.sh', [
            nvmRepo            : NVM_GIT_REPO,
            nvmCommit          : NVM_COMMIT,
            LOG_DELIMITER      : LOG_DELIMITER,
            SCRIPT_LOCATION    : SCRIPT_LOCATION,
            REMOVE_COLOR_CODES : REMOVE_COLOR_CODES,
            nodeVersion        : ext.nodeVersion,
            forever            : FOREVER_EXECUTABLE
        ])

        bashExec( setupScript, taskScriptFile(), false, false )
    }
}
