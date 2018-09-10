/**
 * Console에 Log를 기록
 * @param {string} tag
 * @param {string} message
 */

require('date-utils');
var sprintf = require('sprintf-js').sprintf;

exports.put = function (tag, message) {
	 var now = new Date();
	 console.log(sprintf('%-8s %-19s %s', '[' + tag + ']', '[' + now.toFormat('YY-MM-DD HH24:MI:SS') + ']', message));
}