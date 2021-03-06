package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel


/**
 * Checks that application has started.
 */
class CheckStartedTask extends NodeBaseTask
{
    @Override
    @Requires({ ext.checks })
    void taskAction()
    {
        sleepMs( ext.checkWait * 1000 )

        ext.checks.each {
            String checkUrl, List<?> list ->

            assert checkUrl && list && ( list.size() == 2 )

            final checkPort          = ( ext.publicIp ? checkUrl.find( /:(\d+)/ ){ it[1] } ?: ''                                      : '' )
            final checkPath          = ( ext.publicIp ? checkUrl.replaceFirst( ~'https?://[^/]+', '' )                                : '' )
            final publicUrl          = ( ext.publicIp ? "http://${ ext.publicIp }${ checkPort ? ':' + checkPort : '' }${ checkPath }" : '' )
            final checkStatusCode    = list[ 0 ] as int
            final checkContent       = list[ 1 ] as String
            final response           = httpRequest( checkUrl, 'GET', [:], ext.checkTimeout * 500, ext.checkTimeout * 500, false )
            final responseStatusCode = response.statusCode
            final responseContent    = response.asString()
            final isGoodResponse     = ( responseStatusCode == checkStatusCode ) && contentMatches( responseContent, checkContent, '*' )
            final logMessage         = "Connecting to $checkUrl${ publicUrl ? ' (' + publicUrl + ')' : '' } resulted in " +
                                       (( responseStatusCode instanceof Integer ) ? "status code [$responseStatusCode]" :
                                                                                    "'$responseStatusCode'" ) //  If not Integer then it's an error
            if ( isGoodResponse )
            {
                log{ "$logMessage${ checkContent ? ', content contains [' + checkContent + ']' : '' } - good!" }
            }
            else
            {
                final displayLogStep = 'display application log'
                final errorDetails   = "$logMessage, content [$responseContent] while we expected status code [$checkStatusCode]" +
                                       ( checkContent ? ", content contains [$checkContent]" : '' ) +
                                       ". See '$displayLogStep'."
                final errorMessage   = """
                                       |-----------------------------------------------------------
                                       |  -=-= The application has failed to start properly! =-=-
                                       |-----------------------------------------------------------
                                       |$errorDetails
                                       """.stripMargin()

                log( LogLevel.ERROR ) { errorMessage }

                runTask( LIST_TASK )
                shellExec( tailLogScript(), baseScript( displayLogStep ), scriptFileForTask( 'tail-log' ), false, false, true, false, LogLevel.ERROR )

                if ( ext.stopIfFailsToStart ){ runTask( STOP_TASK )}

                throw new GradleException( errorMessage )
            }
        }
    }


    @SuppressWarnings([ 'LineLength' ])
    /* No private! Called from the closure */
    String tailLogScript()
    {
        // Sorting "forever list" output by processes uptime, taking first element with a minimal uptime and listing its log.
        """
        |echo $LOG_DELIMITER
        |n=`${ forever() } list --plain | $REMOVE_COLOR_CODES | grep -E '\\[[0-9]+\\]' | awk '{print \$NF,\$2}' | sort -n | head -1 | awk '{print \$2}' | tr -d '[]'`
        |echo "${ forever() } logs \$n:"
        |${ forever() } logs \$n${ ext.removeColorCodes }
        |echo $LOG_DELIMITER
        """.stripMargin().toString().trim()
    }
}
