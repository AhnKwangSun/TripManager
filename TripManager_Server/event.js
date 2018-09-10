var Log = require('./io');

/**
 * 에러 코드
 * 1 : 데이터 중복
 */

/**
 * 회원 등록을 수행하는 메소드
 * @param {MySqlConnection} sql
 * @param {JSONObject} json
 * @param {Socket} socket
 */

exports.versionApplication = function (sql, admin, json, socket) {
    var query = 'select `version` from `application`';
    sql.query(query, function (err, rows, fields) {
        var result;
        if (err) {
            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Application Version (' + json.data.version + ')');

            if (rows[0].version == json.data.version)
                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 1
                    }
                };
            else
                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: 1
                    }
                };
        }

        socket.emit('application', result);
    });
}

exports.userApplication = function (sql, admin, json, socket) {
    var query = 'select * from `member` where `kakao_id` = ?';
    sql.query(query, json.data.kakao_id, function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Member Check');

            if (rows.length > 0) {
                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 1,
                    }
                };
            }
            else {
                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0
                    }
                };
            }
        }
        socket.emit('application', result);
    });
}

exports.checkMember = function (sql, admin, json, socket) {
    var query = 'select `name`, convert(`member_picture` using utf8) as `member_picture`, `phone` from `member` where `kakao_id` = ?';
    sql.query(query, json.data.kakao_id, function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };

            socket.emit('member', result);
        }
        else {
            Log.put('QUERY', 'Request - Member Check');

            if (rows.length > 0) {
                query = 'update `member` set `fcm_token` = ? where `kakao_id` = ?';
                sql.query(query, [json.data.fcm_token, json.data.kakao_id], function (err) {
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 1
                            }
                        };
                    }
                    else {
                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1,
                                name: rows[0].name,
                                member_picture: rows[0].member_picture,
                                phone: rows[0].phone
                            }
                        };
                    }

                    socket.emit('member', result);
                });
            }
            else {
                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0
                    }
                };

                socket.emit('member', result);
            }
        }
    });
};

exports.registerMember = function (sql, admin, json, socket) {
    // 등록할 회원 정보를 Object 형식으로 생성
    var member = {
        kakao_id: json.data.kakao_id,
        name: json.data.name,
        member_picture: json.data.member_picture,
        phone: json.data.phone,
        fcm_token: json.data.fcm_token
    };

    var query = 'insert into `member` set ?';
    sql.query(query, member, function (err) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: 1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Member Register');

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 1
                }
            };
        }

        socket.emit('member', result);
    });
};

exports.updateMember = function (sql, admin, json, socket) {
    var member = {
        name: json.data.name,
        member_picture: json.data.member_picture,
        phone: json.data.phone
    };

    var query = 'update `member` set ? where `kakao_id` = ?';
    sql.query(query, [member, json.data.kakao_id], function (err) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: 1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Member Update');

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 1
                }
            };
        }

        socket.emit('member', result);
    });
};

exports.unregisterMember = function (sql, admin, json, socket) {
    var query = 'delete from `join` where member_id = ?';
    sql.query(query, json.data.kakao_id, function (err) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: 1
                }
            };

            socket.emit('member', result);
        }
        else {
            query = 'delete from `editor` where `member_id` = ?';
            sql.query(query, json.data.kakao_id, function (err) {
                /*
                if (err) { }
                else { }
                */

                query = 'delete from `member` where kakao_id = ?';
                sql.query(query, json.data.kakao_id, function (err) {
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 2
                            }
                        };
                    }
                    else {
                        Log.put('QUERY', 'Request - Member Unregister');

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1
                            }
                        };
                    }

                    socket.emit('member', result);
                });
            });
        }
    });
}

exports.createGroup = function (sql, admin, json, socket) {
    // 생성할 그룹 정보를 Object 형식으로 생성
    var group = {
        name: json.data.name,
        group_picture: json.data.group_picture,
        password: json.data.password,
        start_date: json.data.start_date,
        end_date: json.data.end_date + ' 23:59:59',
        leader: json.data.leader
    };

    // `group` 테이블에 그룹 정보를 추가
    var query = 'insert into `group` set ?';
    sql.query(query, group, function (err) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: 1
                }
            };

            socket.emit('group', result);
        }
        else {
            // 직전에 insert된 튜플에 대해 Auto increase된 id값을 가져 옴
            query = 'select last_insert_id() as `id`';
            sql.query(query, function (err, rows, fields) {
                if (err) {
                    Log.put('QUERY', 'Error : ' + err.message);

                    result = {
                        sub_event: json.sub_event,
                        data: {
                            result: 0,
                            code: -1
                        }
                    };

                    socket.emit('group', result);
                }
                else {
                    // 회원이 그룹에 가입한 정보를 Object 형식으로 생성
                    var join = {
                        member_id: json.data.leader,
                        group_id: rows[0].id
                    };

                    // `join` 테이블에 가입 정보를 추가
                    query = 'insert into `join` set ?';
                    sql.query(query, join, function (err) {
                        if (err) {
                            Log.put('QUERY', 'Error : ' + err.message);

                            result = {
                                sub_event: json.sub_event,
                                data: {
                                    result: 0,
                                    code: 2
                                }
                            };

                            socket.emit('group', result);
                        }
                        else {
                            // 리더에 접근 권한 정보를 Object 형식으로 생성
                            var editor = {
                                member_id: json.data.leader,
                                group_id: rows[0].id
                            };

                            query = 'insert into `editor` set ?';
                            sql.query(query, editor, function (err) {
                                if (err) {
                                    Log.put('QUERY', 'Error : ' + err.message);

                                    result = {
                                        sub_event: json.sub_event,
                                        data: {
                                            result: 0,
                                            code: 3
                                        }
                                    };

                                    socket.emit('group', result);
                                }
                                else {
                                    Log.put('QUERY', 'Request - Group Create');

                                    result = {
                                        sub_event: json.sub_event,
                                        data: {
                                            result: 1
                                        }
                                    };
                                }

                                socket.emit('group', result);
                            });
                        }
                    });
                }
            });
        }
    });
};

exports.updateGroup = function (sql, admin, json, socket) {
    var group;

    if (json.data.password == null)
        group = {
            name: json.data.name,
            group_picture: json.data.group_picture,
            start_date: json.data.start_date,
            end_date: json.data.end_date + ' 23:59:59',
            radius: json.data.radius,
            isTracing: json.data.isTracing
        };
    else
        group = {
            name: json.data.name,
            group_picture: json.data.group_picture,
            password: json.data.password,
            start_date: json.data.start_date,
            end_date: json.data.end_date + ' 23:59:59',
            radius: json.data.radius,
            isTracing: json.data.isTracing
        };

    var query = 'update `group` set ? where `id` = ?';
    sql.query(query, [group, json.data.id], function (err) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: 1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Group Update');
            result = {
                sub_event: json.sub_event,
                data: {
                    result: 1
                }
            };
        }

        socket.emit('group', result);
    });
};

exports.findGroup = function (sql, admin, json, socket) {
    var name = '%' + json.data.name + '%';

    // 이름으로 검색한 그룹 목록을 가져옴
    var query = 'select g.`id`, g.`name`, convert(g.`group_picture` using utf8) as `group_picture`, g.`password`, date_format(g.`start_date`, "%Y-%m-%d") as `start_date`, date_format(g.`end_date`, "%Y-%m-%d %T") as `end_date`, g.`radius`, g.`isTracing`, g.`leader`, group_concat(j.`member_id` SEPARATOR ",") as `members` from `group` g join `join` j on g.`id` = j.`group_id` where g.`name` like ? group by(g.`id`)';
    sql.query(query, name, function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Group Find');

            result = {
                sub_event: json.sub_event,
                data: rows
            };
        }

        socket.emit('group', result);
    });
};

exports.joinGroup = function (sql, admin, json, socket) {
    var query = 'select `password` from `group` where `id` = ?'
    sql.query(query, json.data.group_id, function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };

            socket.emit('group', result);
        }
        else {
            if (rows[0].password == json.data.password) {
                // 해당 회원이 그룹에 가입하도록 등록함
                var join = {
                    member_id: json.data.member_id,
                    group_id: json.data.group_id
                };

                query = 'insert into `join` set ?';
                sql.query(query, join, function (err) {
                    var result;
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 2
                            }
                        };
                    }
                    else {
                        Log.put('QUERY', 'Request - Group Join');

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1
                            }
                        };
                    }

                    socket.emit('group', result);
                });
            }
            else {
                Log.put('QUERY', 'Error : Wrong password');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: 1
                    }
                };

                socket.emit('group', result);
            }
        }
    });
};

exports.exitGroup = function (sql, admin, json, socket) {
    var query = 'delete from `join` where `member_id` = ? and `group_id` = ?';
    sql.query(query, [json.data.member_id, json.data.group_id], function (err) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: 1
                }
            };

            socket.emit('group', result);
        }
        else {
            query = 'delete from `editor` where `member_id` = ? and `group_id` = ?';
            sql.query(query, [json.data.member_id, json.data.group_id], function (err) {
                /*
                if (err) { }
                else { }
                */

                Log.put('QUERY', 'Request - Group Exit');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 1
                    }
                };

                socket.emit('group', result);
            });
        }
    });
};

exports.displayGroup = function (sql, admin, json, socket) {
    // 해당 회원이 참가한 모든 그룹의 정보를 가져옴
    var query = 'select g.`id`, g.`name`, g.`group_picture`, convert(g.`group_picture` using utf8) as `group_picture`, date_format(g.`start_date`, "%Y-%m-%d") as `start_date`, date_format(g.`end_date`, "%Y-%m-%d %T") as `end_date`, g.`radius`, g.`isTracing`, g.`leader`, group_concat(DISTINCT j.`member_id` SEPARATOR ",") as `members` from `group` g join `join` j on g.`id` = j.`group_id` where g.`id` in (select`group_id` from `join` where `member_id` = ?) group by g.`id`';
    sql.query(query, json.data.kakao_id, function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Group Display');

            result = {
                sub_event: json.sub_event,
                data: rows
            };
        }

        socket.emit('group', result);
    });
};

exports.getEditorGroup = function (sql, admin, json, socket) {
    var query = 'select * from `editor` where `group_id` = ?';
    sql.query(query, json.data.group_id, function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Group getEditor');

            result = {
                sub_event: json.sub_event,
                data: rows
            };
        }

        socket.emit('group', result)
    });
}

exports.createSchedule = function (sql, admin, json, socket) {
    var query = 'select * from `editor` where `member_id` = ? and `group_id` = ?'
    sql.query(query, [json.data.member_id, json.data.group_id], function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };

            socket.emit('schedule', result);
        }
        else {
            if (rows.length > 0) {
                var schedule = {
                    date: json.data.date,
                    content: json.data.content,
                    group_id: json.data.group_id
                };

                query = 'insert into `schedule` set ?';
                sql.query(query, schedule, function (err) {
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 2
                            }
                        };
                    }
                    else {
                        Log.put('QUERY', 'Request - Schedule Create');

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1
                            }
                        };
                    }

                    socket.emit('schedule', result);

                    /* 새로운 일정이 등록되면 푸시메시지 알림 */
					query = 'select `fcm_token` from `member` where `kakao_id` in (select `member_id` from `join` where `group_id` = ?) and not `kakao_id` = ?';
					sql.query(query, [json.data.group_id, json.data.member_id], function (err, rows, fields) {
						if (err) {
							// select query error
						}
						else {
							var fcm_message = {
								data: {
									title: '새로운 일정이 있습니다.',
									group_id: '' + json.data.group_id
								}
							};

							var length = rows.length * 1;
							for (var i = 0; i < rows.length; i++)
								admin.messaging().sendToDevice(rows[i].fcm_token, fcm_message);
						}
					});
                });
            }
            else {
                Log.put('QUERY', 'Error : No authority');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: 1
                    }
                };

                socket.emit('schedule', result);
            }
        }
    });
};

exports.updateSchedule = function (sql, admin, json, socket) {
    var query = 'select * from `editor` where `member_id` = ? and `group_id` = ?'
    sql.query(query, [json.data.member_id, json.data.group_id], function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };

            socket.emit('schedule', result);
        }
        else {
            if (rows.length > 0) {
                var schedule = {
                    date: json.data.date,
                    content: json.data.content
                };

                query = 'update `schedule` set ? where `id` = ?';
                sql.query(query, [schedule, json.data.id], function (err) {
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 2
                            }
                        };
                    }
                    else {
                        Log.put('QUERY', 'Request - Schedule Update');

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1
                            }
                        };
                    }

                    socket.emit('schedule', result);
                });
            }
            else {
                Log.put('QUERY', 'Error : No authority');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: 1
                    }
                };

                socket.emit('schedule', result);
            }
        }
    });
};

exports.deleteSchedule = function (sql, admin, json, socket) {
    var query = 'select * from `editor` where `member_id` = ? and `group_id` = ?'
    sql.query(query, [json.data.member_id, json.data.group_id], function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };

            socket.emit('schedule', result);
        }
        else {
            if (rows.length > 0) {
                query = 'delete from `schedule` where `id` = ?';
                sql.query(query, json.data.id, function (err) {
                    var result;
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 2
                            }
                        };
                    }
                    else {
                        Log.put('QUERY', 'Request - Schedule Delete');

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1
                            }
                        };
                    }

                    socket.emit('schedule', result);
                });
            }
            else {
                Log.put('QUERY', 'Error : No authority');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: 1
                    }
                };

                socket.emit('schedule', result);
            }
        }
    });
};

exports.displaySchedule = function (sql, admin, json, socket) {
    var query = 'select `id`, date_format(`date`, "%Y-%m-%d %H:%i") as `date`, `content` from `schedule` where `group_id` = ? order by `date`';
    sql.query(query, json.data.id, function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Schedule Display');

            result = {
                sub_event: json.sub_event,
                data: rows
            };
        }

        socket.emit('schedule', result);
    });
};

exports.createNotification = function (sql, admin, json, socket) {
    var query = 'select * from `editor` where `member_id` = ? and `group_id` = ?'
    sql.query(query, [json.data.member_id, json.data.group_id], function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };

            socket.emit('notification', result);
        }
        else {
            if (rows.length > 0) {
                query = 'insert into `notification`(`date`, `content`, `group_id`, `member_id`) values (now(), ?, ?, ?)';
                sql.query(query, [json.data.content, json.data.group_id, json.data.member_id], function (err) {
                    var result;
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 2
                            }
                        };
                    }
                    else {
                        Log.put('QUERY', 'Request - Notification Create');

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1
                            }
                        };
                    }

                    socket.emit('notification', result);

                    /* 새로운 공지사항이 등록되면 푸시메시지 알림 */
					query = 'select `fcm_token` from `member` where `kakao_id` in (select `member_id` from `join` where `group_id` = ?) and not `kakao_id` = ?';
					sql.query(query, [json.data.group_id, json.data.member_id], function (err, rows, fields) {
						if (err) {
							// select query error
						}
						else {
							var fcm_message = {
								data: {
									title: '새로운 공지사항이 있습니다.',
									group_id: '' + json.data.group_id
								}
							};

							var length = rows.length * 1;
							for (var i = 0; i < rows.length; i++)
								admin.messaging().sendToDevice(rows[i].fcm_token, fcm_message);
						}
					});
                });
            }
            else {
                Log.put('QUERY', 'Error : No authority');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: 1
                    }
                };

                socket.emit('notification', result);
            }
        }
    });
};

exports.updateNotification = function (sql, admin, json, socket) {
    var query = 'select * from `editor` where `member_id` = ? and `group_id` = ?'
    sql.query(query, [json.data.member_id, json.data.group_id], function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };

            socket.emit('notification', result);
        }
        else {
            if (rows.length > 0) {
                query = 'update `notification` set `content` = ? where `id` = ?';
                sql.query(query, [json.data.content, json.data.id], function (err) {
                    var result;
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 2
                            }
                        };
                    }
                    else {
                        Log.put('QUERY', 'Request - Notification Update');

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1
                            }
                        };
                    }

                    socket.emit('notification', result);
                });
            }
            else {
                Log.put('QUERY', 'Error : No authority');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: 1
                    }
                };

                socket.emit('notification', result);
            }
        }
    });
};

exports.deleteNotification = function (sql, admin, json, socket) {
    var query = 'select * from `editor` where `member_id` = ? and `group_id` = ?'
    sql.query(query, [json.data.member_id, json.data.group_id], function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };

            socket.emit('notification', result);
        }
        else {
            if (rows.length > 0) {
                var query = 'update `notification` set `isDeleted` = 1 where `id` = ?';
                sql.query(query, json.data.id, function (err) {
                    var result;
                    if (err) {
                        Log.put('QUERY', 'Error : ' + err.message);

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 0,
                                code: 2
                            }
                        };
                    }
                    else {
                        Log.put('QUERY', 'Request - Notification Delete');

                        result = {
                            sub_event: json.sub_event,
                            data: {
                                result: 1
                            }
                        };
                    }

                    socket.emit('notification', result);
                });
            }
            else {
                Log.put('QUERY', 'Error : No authority');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: 1
                    }
                };

                socket.emit('notification', result);
            }
        }
    });
};

exports.displayNotification = function (sql, admin, json, socket) {
    var query = 'select m.`kakao_id`, m.`name`, convert(`member_picture` using utf8) as `member_picture`, n.`id`, date_format(n.`date`, "%Y-%m-%d %H:%i") as `date`, n.`content` from `notification` n left join `member` m on n.member_id = m.kakao_id where n.`group_id` = ? and n.`isDeleted` = 0 order by `date` DESC';
    sql.query(query, json.data.id, function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Notification Display');

            result = {
                sub_event: json.sub_event,
                data: rows
            };
        }

        socket.emit('notification', result);
    });
};

exports.findFollower = function (sql, admin, json, socket) {
    var name = '%' + json.data.name + '%';

    var query = 'select m.`kakao_id`, m.`name`, convert(`member_picture` using utf8) as `member_picture`, m.`phone` from `member` m join `join` j on m.`kakao_id` = j.`member_id` where j.`group_id` = ? and m.`name` like ? ';
    sql.query(query, [json.data.id, name], function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Follower Find');

            result = {
                sub_event: json.sub_event,
                data: rows
            };
        }

        socket.emit('follower', result);
    });
};

exports.displayFollower = function (sql, admin, json, socket) {
    var query = 'select m.`kakao_id`, m.`name`, convert(`member_picture` using utf8) as `member_picture`, m.`phone` from `member` m join `join` j on m.`kakao_id` = j.`member_id` where j.`group_id` = ? order by m.`name` ASC';
    sql.query(query, json.data.id, function (err, rows, fields) {
        var members = rows;

        var query = 'select `member_id` from `editor` where `group_id` = ?';
        sql.query(query, json.data.id, function (err, rows, fileds) {
            var result;
            if (err) {
                Log.put('QUERY', 'Error : ' + err.message);

                result = {
                    sub_event: json.sub_event,
                    data: {
                        result: 0,
                        code: -1
                    }
                };
            }
            else {
                Log.put('QUERY', 'Request - Follower Display');

                result = {
                    sub_event: json.sub_event,
                    data: {
                        members: members,
                        editors: rows
                    }
                };
            }

            socket.emit('follower', result);
        });
    });
};

exports.traceFollower = function (sql, admin, json, socket) {
    var member = {
        latitude: json.data.latitude,
        longitude: json.data.longitude
    };

    var query = 'update `member` set ? where `kakao_id` = ?';
    sql.query(query, [member, json.data.kakao_id], function (err) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: 1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Follower Trace');

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 1
                }
            };
        }

        socket.emit('follower', result);
    });
}

exports.getLatlngFollower = function (sql, admin, json, socket) {
    var query = 'select m.`latitude`, m.`longitude`, g.`id`, date_format(g.`start_date`, "%Y-%m-%d") as `start_date`, date_format(g.`end_date`, "%Y-%m-%d") as `end_date`, g.`isTracing`, g.`radius` from `group` g join `member` m on g.`leader` = m.`kakao_id` where g.`id` in (select j.`group_id` from `join` j join `group` g on j.`group_id` = g.`id` where `member_id` = ? and not g.`leader` = ?);';

    sql.query(query, [json.data.kakao_id, json.data.kakao_id], function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Follower GetLatLng');

            result = {
                sub_event: json.sub_event,
                data: rows
            };
        }

        socket.emit('follower', result);
    });
}

exports.alramFollower = function (sql, admin, json, socket) {
		var query = 'select `fcm_token` from `member` where `kakao_id` in (select `leader` from `group` where `id` = ?);'
		sql.query(query, json.data.group_id, function (err, rows, fields) {

			if (err)
				Log.put('QUERY', 'Error : ' + err.message);
			else {
				Log.put('QUERY', 'Request - Follower Alram');

				var fcm_message = {
					data: {
						title: '반경을 이탈한 사용자가 있습니다.',
						group_id: '' + json.data.group_id
					}
				};

				admin.messaging().sendToDevice(rows[0].fcm_token, fcm_message);
			}
		});
}

exports.locationFollower = function (sql, admin, json, socket) {
    var query;
    if (json.data.flag == 1)
        query = 'select `kakao_id`, `name`, `phone`, `latitude`, `longitude` from `member` where `kakao_id` in (select `member_id` from `join` where `group_id` = ?)';
    else
        query = 'select `kakao_id`, `name`, `phone`, `latitude`, `longitude` from `member` where `kakao_id` in (select `leader` from `group` where `id` = ?)';

    sql.query(query, json.data.group_id, function (err, rows, fields) {
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: -1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Follower Location');

            result = {
                sub_event: json.sub_event,
                data: rows
            };
        }

        socket.emit('follower', result);
    });
}

exports.accessibleFollower = function (sql, admin, json, socket) {
    var query;
    if (json.data.accessible)
        query = 'insert into `editor` values (?, ?)';
    else
        query = 'delete from `editor` where `member_id` = ? and `group_id` = ?';

    sql.query(query, [json.data.member_id, json.data.group_id], function (err, rows, fields) {
        var result;
        if (err) {
            Log.put('QUERY', 'Error : ' + err.message);

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 0,
                    code: 1
                }
            };
        }
        else {
            Log.put('QUERY', 'Request - Follower Accessible');

            result = {
                sub_event: json.sub_event,
                data: {
                    result: 1
                }
            };
        }

        socket.emit('follower', result);
    });
}

exports.GPSBoostFollower = function (sql, admin, json, socket) {
	var query = 'select `fcm_token` from `member` where `kakao_id` in (select `member_id` from `join` where `group_id` = ?) and not `kakao_id` = ?';
	sql.query(query, [json.data.group_id, json.data.member_id], function (err, rows, fields) {
		if (err) {
			// select query error
		}
		else {
			Log.put('QUERY', 'Request - Follower Boost');
	
			var fcm_message;

			if (json.data.boost)
				fcm_message = {
					data: {
						boost: '1'
					}
				};
			else
				fcm_message = {
					data: {
						boost: '0'
					}
				};

			var length = rows.length * 1;
			for (var i = 0; i < rows.length; i++)
				admin.messaging().sendToDevice(rows[i].fcm_token, fcm_message);
		}
	});
}
