(function() {

    var url = 'http://localhost:8080/request?name=';
    var msgIndex, key;
    var botui = new BotUI('final-project');
    var searchType;
    var searchField;
    var searchWord;

    // Initial message
    botui.message.bot({
        content: 'This is my final project chatbot.'
    }).then(init);

    function init() {
        // Brute Force search
        botui.message.bot({
            content: 'Click search type for Brute Force(Ex. HeartRate)'
        }).then(typeSelect).then(function(res) {
            searchType = res.value
            return botui.message.bot({
                content: res.value + ' selected.'
            })
        }).then(function(res) {
            return botui.message.bot({
                content: 'Enter search word.'
            })
        }).then(function(res) {
            return botui.action.text({
                action: {
                    placeholder: 'Ex.: "bpm":75'
                }
            });
        }).then(function(res) {
            searchWord = res.value;
            getBruteForceResult();
            botui.message.bot({
                loading: true
            }).then(function(index) {
                // Get the index of loading icon
                // Update the message info using this index
                // Otherwise the icon does not disappear
                msgIndex = index;
            })
        }).then(function(res) {
            return botui.message.bot({
                content: 'Click search type for Lucene(Ex. ActivFit)'
            })
        }).then(typeSelect).then(function(res) {
            searchType = res.value
            return botui.message.bot({
                content: res.value + ' selected.'
            })
        }).then(function(res) {
            return botui.message.bot({
                content: 'Enter search word.'
            })
        }).then(function(res) {
            return botui.action.text({
                action: {
                    placeholder: 'Ex.: running'
                }
            });
        }).then(function(res) {
            searchWord = res.value;
            getLuceneResult();
            botui.message.bot({
                loading: true
            }).then(function(index) {
                // Get the index of loading icon
                // Update the message info using this index
                // Otherwise the icon does not disappear
                msgIndex = index;
            })
        }).then(function(res) {
            return botui.message.bot({
                content: 'Click search type for MongoDB(Ex. SA/LightSensor)'
            })
        }).then(typeSelect).then(function(res) {
            searchType = res.value
            return botui.message.bot({
                content: res.value + ' selected.'
            })
        }).then(function(res) {
            return botui.message.bot({
                content: 'Enter search field.'
            })
        }).then(function(res) {
            return botui.action.text({
                action: {
                    placeholder: 'Ex.: data'
                }
            });
        }).then(function(res) {
            searchField = res.value
            return botui.message.bot({
                content: 'Enter search word.'
            })
        }).then(function(res) {
            return botui.action.text({
                action: {
                    placeholder: 'Ex.: light'
                }
            });
        }).then(function(res) {
            searchWord = res.value;
            getMongoDBResult();
            botui.message.bot({
                loading: true
            }).then(function(index) {
                // Get the index of loading icon
                // Update the message info using this index
                // Otherwise the icon does not disappear
                msgIndex = index;
            })
        }).then(function(res) {
            return botui.message.bot({
                content: 'Click search type for MySQL(Ex. HeartRate)'
            })
        }).then(typeSelect).then(function(res) {
            searchType = res.value
            return botui.message.bot({
                content: res.value + ' selected.'
            })
        }).then(function(res) {
            return botui.message.bot({
                content: 'Enter search field.'
            })
        }).then(function(res) {
            return botui.action.text({
                action: {
                    placeholder: 'Ex.: sensor_data.bpm'
                }
            });
        }).then(function(res) {
            searchField = res.value
            return botui.message.bot({
                content: 'Enter search word.'
            })
        }).then(function(res) {
            return botui.action.text({
                action: {
                    placeholder: 'Ex.: 75'
                }
            });
        }).then(function(res) {
            searchWord = res.value;
            getMySQLResult();
            botui.message.bot({
                loading: true
            }).then(function(index) {
                // Get the index of loading icon
                // Update the message info using this index
                // Otherwise the icon does not disappear
                msgIndex = index;
            })
        }).then(function() {
            return botui.message.bot({
                content: 'Retry?'
            })
        }).then(function() {

            return botui.action.button({
                action: [{
                    icon: 'circle-thin',
                    text: 'Yes',
                    value: true
                }, {
                    icon: 'close',
                    text: 'No',
                    value: false
                }]
            });
        }).then(function(res) {
            //「続ける」か「終了」するかの条件分岐処理
            res.value ? init() : showCharts();
        });
    }

    // Get Brute Force result
    function getBruteForceResult() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'http://localhost:8080/getBruteForceResult?searchType=' + searchType + '&searchWord=' + searchWord);
        xhr.onload = function() {
            var result = JSON.parse(xhr.responseText);
            showMessage(result.content);
        }
        xhr.send();
    }

    // Get Lucene result
    function getLuceneResult() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'http://localhost:8080/getLuceneResult?searchType=' + searchType + '&searchWord=' + searchWord);
        xhr.onload = function() {
            var result = JSON.parse(xhr.responseText);
            showMessage(result.content);
        }
        xhr.send();
    }

    // Get MongoDB result
    function getMongoDBResult() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'http://localhost:8080/getMongoDBResult?searchType=' + searchType + '&searchField=' + searchField + '&searchWord=' + searchWord);
        xhr.onload = function() {
            var result = JSON.parse(xhr.responseText);
            showMessage(result.content);
        }
        xhr.send();
    }

    // Get MySQL result
    function getMySQLResult() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'http://localhost:8080/getMySQLResult?searchType=' + searchType + '&searchField=' + searchField + '&searchWord=' + searchWord);
        xhr.onload = function() {
            var result = JSON.parse(xhr.responseText);
            showMessage(result.content);
        }
        xhr.send();
    }

    var typeSelect = function() {
        return botui.action.button({
            addMessage: true,
            // so we could the address in message instead if 'Existing Address'
            action: [{
                text: 'ActivFit',
                value: 'ActivFit'
            }, {
                text: 'HeartRate',
                value: 'HeartRate'
            }, {
                text: 'SA/LightSensor',
                value: 'SA/LightSensor'
            }]
        })
    }

    function showMessage(result) {
        // Update the message by using the index of loading icon
        botui.message.update(msgIndex, {
            content: 'Result for ' + searchType + ': <br><br>' + result
        })
    }

    function showCharts() {
        botui.message.bot({
            content: 'Ok, I will show you performance charts.'
        }).then(function(res) {
            return botui.message.bot({
                delay: 1000,
                content: '<img src=image?type=SA/LightSensor>'
            })
        }).then(function(res) {
            return botui.message.bot({
                delay: 1000,
                content: '<img src=image?type=ActivFit>'
            })
        }).then(function(res) {
            botui.message.bot({
                delay: 1000,
                content: '<img src=image?type=HeartRate>'
            });
            end();
        });
    }

    function end() {
        botui.message.bot({
            content: 'Thank you for using!'
        })
    }

}
)();
