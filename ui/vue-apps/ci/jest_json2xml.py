#!/usr/bin/env python

import sys
import json
import xml.etree.ElementTree as ET



def main(jest_results_path, out_path):
    with open(jest_results_path, 'r') as infile:
        results = json.loads(infile.read())
    tree = ET.Element('testsuites',
                      name='jest tests',
                      tests=str(results['numTotalTests']),
                      failures=str(results['numFailedTests']),
                      errors=str(results['numRuntimeErrorTestSuites']))

    for suite in results['testResults']:
        suite_name = suite['name'].split('/')[-1]
        suite_el = ET.SubElement(tree,
                                 'testsuite',
                                 name=suite_name,
                                 package=suite_name,
                                 tests=str(len(suite['assertionResults'])))

        for test in suite['assertionResults']:
            test_el = ET.SubElement(suite_el,
                                    'testcase',
                                    classname=test['title'],
                                    name=test['title'])
            if test['status'] != 'passed':
                ET.SubElement(test_el,
                              'failure',
                              message=str(test['failureMessages']))

        if not suite['assertionResults']:
            test_el = ET.SubElement(suite_el,
                                    'testcase',
                                    classname='dummy_wrap',
                                    name='dummy_wrap')
            ET.SubElement(test_el,
                          'failure',
                          message=suite['message'])



    ET.ElementTree(tree).write(open(out_path, 'wb'))
    if results['numFailedTestSuites'] == results['numFailedTests'] == 0:
        print('SUCCESS')
    else:
        print('UNSTABLE')


def usage():
    print('Usage:')
    print('    {} <jest results path> <xml output file>'.format(sys.argv[0]))

if __name__ == '__main__':
    if len(sys.argv) == 3:
        main(sys.argv[1], sys.argv[2])
    else:
        usage()
