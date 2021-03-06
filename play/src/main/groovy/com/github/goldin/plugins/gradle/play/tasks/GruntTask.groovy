package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import org.gcontracts.annotations.Requires


/**
 * Runs various Grunt operations
 */
class GruntTask extends PlayBaseTask
{
    @Override
    void taskAction()
    {
        generatePackageJson()
        generateGruntFile()
    }


    private void generatePackageJson()
    {
        final variables = ( Map<String,?> ) ext.versions.collectEntries { String key, String value ->
            [ key.replace( '-', '_' ), value ]
        } + [ name    : project.name,
              version : '0.0.1' ]

        writeTemplate( PACKAGE_JSON, PACKAGE_JSON, variables )
    }


    private void generateGruntFile()
    {
        final Map<String, List<String>> coffeeFiles         = [:]
        final Map<String, List<String>> coffeeFilesMinified = [:]
        final Map<String, List<String>> lessFiles           = [:]
        final Map<String, List<String>> lessFilesMinified   = [:]

        for ( Map<String,?> map in ext.grunt )
        {
            final Object  source      = map[ 'src' ]
            final String  destination = map[ 'dest' ]
            final boolean minify      = map[ 'minify' ]

            assert source && destination, \
                   "Both 'src' and 'dest' should be defined for each 'grunt' element"

            assert ( source instanceof String ) || ( source instanceof List ), \
                   "Illegal source '${source}' of type '${ source.getClass().name }' - " +
                   "should be of type '${ String.name }' or '${ List.name }'"

            final isJs  = destination.endsWith( '.js' )
            final isCss = destination.endsWith( '.css' )

            assert isJs || isCss, "Illegal grunt destination '${destination}' - should end with either '.js' or '.css'"

            isJs? updateFiles( DOT_JS,  DOT_COFFEE, source, destination, minify, coffeeFiles, coffeeFilesMinified ) :
                  updateFiles( DOT_CSS, DOT_LESS,   source, destination, minify, lessFiles,   lessFilesMinified   )
        }

        final destinations = [ coffeeFiles, coffeeFilesMinified, lessFiles, lessFilesMinified ].collect { it.keySet() }.flatten()
        final variables    = [ packageJson         : PACKAGE_JSON,
                               destinations        : destinations,
                               coffeeFiles         : coffeeFiles,
                               coffeeFilesMinified : coffeeFilesMinified,
                               lessFiles           : lessFiles,
                               lessFilesMinified   : lessFilesMinified ]

        writeTemplate( GRUNT_FILE, GRUNT_FILE, variables )
    }


    @Requires({ extension && compiledExtension && source && destination.endsWith( extension ) &&
                ( files != null ) && ( minifiedFiles != null ) })
    private void updateFiles ( String                    extension,
                               String                    compiledExtension,
                               Object                    source,
                               String                    destination,
                               boolean                   isMinifyFiles,
                               Map<String, List<String>> files,
                               Map<String, List<String>> minifiedFiles )
    {
        final isPath = {
            String path ->
            final f = project.file( path )
            f.file ? path.endsWith( compiledExtension ) : f.list().any{ it.endsWith( compiledExtension ) }
        }

        final getPath = {
            String path ->
            final f = project.file( path )
            f.file ? ( path.endsWith( compiledExtension ) ? path : null ) : "${ path }${ path.endsWith( '/' ) ? '' : '/' }*${ compiledExtension }"
        }

        final isSourceList = source instanceof List
        final isFiles      = isSourceList ? source.any{ String s -> isPath( s )} :
                                            isPath(( String ) source )
        if ( isFiles )
        {
            files[ ( destination ) ] = ( isSourceList ? source.collect { String s -> getPath( s ) } :
                                                        [ getPath(( String ) source ) ] ).grep()
            if ( isMinifyFiles )
            {
                final destinationMinified                = destination[ 0 .. -( "$extension".size() + 1 ) ] + ".min$extension"
                minifiedFiles[ ( destinationMinified ) ] = [ destination ]
            }
        }
    }
}
