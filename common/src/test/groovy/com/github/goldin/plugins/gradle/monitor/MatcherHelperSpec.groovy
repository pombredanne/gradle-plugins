package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.BaseSpecification
import com.github.goldin.plugins.gradle.common.helpers.MatcherHelper


/**
 * {@link MatcherHelper} test spec.
 */
class MatcherHelperSpec extends BaseSpecification
{
    @Delegate final MatcherHelper helper = new MatcherHelper()

    def 'Content matches single matcher'()
    {
        expect:
        contentMatches( content, pattern, '' )

        where:
        content | pattern
        ''      | ''
        'aaaaa' | ''
        'aaaaa' | 'a'
        'aaaaa' | 'aa'
        'aaaaa' | 'aaa'
        'aaaaa' | 'aaaa'
        'aaaaa' | 'aaaaa'
        'aaaaa' | '-aaaaaa'
        'aaaaa' | '-b'
        'aaaaa' | '-1234'
        'aaaaa' | '/a/'
        'aaaaa' | '/a+/'
        'aaaaa' | '/^a+$/'
        'aaaaa' | '/^a{5}$/'
        'aaaaa' | '-/^b{5}$/'
        'aaaaa' | '-/\\d/'
        'aaaaa' | '/\\w/'
        'aaaaa' | '/\\w{5}/'
    }


    def 'Content matches multiple matchers'()
    {
        expect:
        contentMatches( content, pattern, '*' )

        where:
        content | pattern
        ''      | '*******'
        'aaaaa' | '*******'
        'aaaaa' | 'a*-b*-c*/a/*-/c/'
        'aaaaa' | '-b*-1234*/^a+$/*'
        'aaaaa' | '/^a{5}$/*-/^b{5}$/*-/\\d/'
        'aaaaa' | '/\\w/*/\\w{5}/'
    }


    @SuppressWarnings([ 'LineLength' ])
    def 'Content matches JSON matchers'()
    {
        expect:
        contentMatches( content, pattern, '*' )

        where:
        content | pattern
        '{ "a" : "b", "c" : "d" }' | '{ "a" : "b" }'
        '{ "a" : [ "b" ], "c" : "d" }' | '{ "a" : [ "b" ] }'
        '{ "a" : { "b" : "c", "d" : "e", "k" : "m" }, "c" : "d", "f": "g" }' | '{ "a" : { "b" : "c", "d" : "e" }, "f" : "g" }'
        '{ "a" : [{ "b" : "c", "d" : "e", "k" : "m" }], "c" : "d", "f": "g" }' | '{ "a" : [{ "b" : "c", "d" : "e" }], "f" : "g" }'
        '{ "a" : "b", "c" : "d" }' | '{ "a" : "b" }*{ "c" : "d" }*{ "a" : "b", "c" : "d" }'
        '{ "a" : [ "b" ], "c" : "d" }' | '{ "a" : [ "b" ] }*{ "c" : "d" }*{ "a" : [ "b" ], "c" : "d" }'
        '{ "a" : { "b" : "c", "d" : "e", "k" : "m" }, "c" : "d", "f": "g" }' | '{ "a" : { "b" : "c", "d" : "e" }, "f" : "g" }*{ "a" : { "k" : "m" }}*{ "a" : { "b" : "c", "d" : "e", "k" : "m" }, "c" : "d", "f": "g" }'
        '{ "a" : [{ "b" : "c", "d" : "e", "k" : "m" }], "c" : "d", "f": "g" }' | '{ "a" : [{ "b" : "c", "d" : "e" }], "f" : "g" }*{ "c" : "d" }*{ "f" : "g" }*{ "a" : [{ "b" : "c", "d" : "e", "k" : "m" }], "c" : "d", "f": "g" }'
        '[ "a", "b", "c" ]' | '[ "a" ]'
        '[ "a", "b", "c" ]' | '[ "a" ]*[ "b" ]*[ "c" ]*[ "a", "b", "c" ]'
        '[ "a", "b", { "c": { "d": { "e": [ "f", "g" ] }}} ]' | '[ "a" ]*[ "b" ]*[ { "c": { "d": { "e": [ "f", "g" ] }}} ]*[ "a", "b", { "c": { "d": { "e": [ "f", "g" ] }}} ]'
        '{ "a" : [{ "b" : 100, "d" : true, "k" : false }], "100" : 200, "true": false }' | '{ "a" : [{ "b" : 100, "d" : true }], "100" : 200 }*{ "100" : 200 }*{ "true" : false }*{ "a" : [{ "b" : 100, "d" : true, "k" : false }], "100" : 200, "true": false }'
        '[ "a", 1, { "c": { "d": { "e": [ true, false ] }}} ]' | '[ "a" ]*[ 1 ]*[ { "c": { "d": { "e": [ true ] }}} ]*[ 1 ]*[ { "c": { "d": { "e": [ false ] }}} ]*[ "a", 1, { "c": { "d": { "e": [ true, false ] }}} ]'
        '{ "a" : "b", "c" : "d" }' | '-{ "a" : "b1" }'
        '{ "a" : [ "b" ], "c" : "d" }' | '-{ "a" : [ "e" ] }'
        '{ "a" : { "b" : "c", "d" : "e", "k" : "m" }, "c" : "d", "f": "g" }' | '-{ "a" : { "b" : "c", "d" : "e1" }, "f" : "g" }'
        '{ "a" : [{ "b" : "c", "d" : "e", "k" : "m" }], "c" : "d", "f": "g" }' | '-{ "a" : [{ "b" : "c", "d2" : "e" }], "f" : "g" }'
        '{ "a" : "b", "c" : "d" }' | '{ "a" : "b" }*-{ "c2" : "d" }*-{ "a1" : "b", "c" : "d" }'
        '{ "a" : [ "b" ], "c" : "d" }' | '-{ "a" : { "b" : "b" }}*-{ "c" : [ "d" ]}*-{ "a" : [ "b" ], "c" : [ "d" ]}'
        '{ "a" : { "b" : "c", "d" : "e", "k" : "m" }, "c" : "d", "f": "g" }' | '-{ "a" : [ "b", "c", "d", "e" ], "f" : "g" }*{ "a" : { "k" : "m" }}*-{ "a" : { "b" : "c", "d" : "e", "k" : "m" }, "c" : "d", "f": [ "g" ]}'
        '{ "a" : [{ "b" : "c", "d" : "e", "k" : "m" }], "c" : "d", "f": "g" }' | '-{ "a" : [{ "b" : "c", "d" : "e2" }], "f" : "g" }*-[ "c", "d" ]*{ "f" : "g" }*-{ "a" : [{ "b" : "c", "d" : "e", "k" : "m", "c" : [ "d", "f", "g" ] }]}'
        '[ "a", "b", "c" ]' | '-[ "d" ]'
        '[ "aa", "bb", "cc" ]' | '-[ "a" ]*-[ "b" ]*-[ "c" ]*-[ "a", "b", "c" ]'
        '[ "a", "b", { "c": { "d": { "e": [ "f", "g" ] }}} ]' | '-[ "e" ]*-[ "h" ]*-[ { "c": { "d": { "e": [ "f", "g", "u" ] }}} ]*-[ "a", "b", { "c": { "d": { "e": { "f" : ["g"] }}}} ]'
        '{ "a" : "true", "b" : "false" }' | '{ "a" : "true" }*{ "b" : "false" }*-{ "a" : "false" }*-{ "b" : "true" }*-{ "a" : "false", "b": "true" }'
        '{ "a" : "true", "b" : "false" }' | '-{ "a" : true }*-{ "b" : false }*-{ "a" : true, "b": false }*-{ "a" : false, "b": true }'
        '[ "true", "false" ]' | '[ "true" ]*[ "false" ]*-[ true ]*-[ false ]'
    }
}
