/**
 * @author GBKSOFT team <hello@gbksoft.com>
 * @link http://gbksoft.com
 * @copyright 2011-2015 GBKSOFT team
 * @since 1.0
 *
 */

app.controller('UsersIndex', [
    '$scope', 'rest', 'toaster', '$modal', 'authProvider', 'constantsGetter', 'CONSTANTS', 'ROLES', 'callback',
    function ($scope, rest, toaster, $modal, authProvider, constantsGetter, CONSTANTS, ROLES, callback)
    {
        $scope.constantsGetter = constantsGetter; //constants factory
        $scope.CONSTANTS = CONSTANTS; //constants
        $scope.models = [];
        $scope.model = {};
        $scope.searchmodel = {}; //search model
        $scope.network = authProvider.getNetwork();
        $scope.model.role = {};
        $scope.temp = {};
        $scope.temp.details = '';
        $scope.editModal = $modal({
            scope: $scope,
            template: 'modules/users/views/edit.html',
            show: false
        });

        // create watcher on email change, when new resume created
        $scope.$on('modal.hide.before', function () {
            //unwatch
            $scope.endWatchEmail();
        });

        $scope.startWatchEmail = function () {
            $scope.endWatchEmail = $scope.$watch('model.email', function (value, oldvalue) {
                if (value != oldvalue) {
                    $scope.searchInOtherNetworks();
                }
            });
        };

        $scope.searchInOtherNetworks = function () {
            rest.path = 'user/searchInOtherNetworks';
            rest.search({email: $scope.model.email}).success(function (data) {
                if (data.result.id) {
                    $scope.model = data.result;
                }
            }).error(callback.error)
        };

        $scope.goPage = function (page) {
            $scope.searchmodel.page = page;
            //search all
            $scope.search();
        };

        $scope.search = function () {
            rest.path = 'user';
            rest.search($scope.searchmodel).success(function (data) {
                $scope.models = data.result.users;

                //pagination
                if (data.result.pagination) {
                    $scope.page = data.result.pagination.page;
                    $scope.pageCount = data.result.pagination.pageCount;
                    $scope.perPage = data.result.pagination.perPage;
                    $scope.totalCount = data.result.pagination.totalCount;

                    $scope.from = ($scope.page - 1) * $scope.perPage + 1;
                    $scope.to = $scope.page * $scope.perPage;
                    if ($scope.to > $scope.totalCount) {
                        $scope.to = $scope.totalCount;
                    }
                }
            }).error(callback.error)
        }

        $scope.setModelById = function (id) {
            $scope.model = {};
            $scope.models.forEach(function (item) {
                if (item.id == id) {
                    $scope.model = item;
                }
            });
        };

        $scope.fileChanged = function (element) {
            var file = element.files[0];
            var oFReader = new FileReader();
            oFReader.readAsDataURL(file);
            oFReader.onload = function (oFREvent) {
                document.getElementById("uploadPreview").src = oFREvent.target.result;
            };

            $scope
            .$apply(function ($scope) {
                $scope.model.image = file;
            }.bind(this));
        };

        $scope.create = function () {
            $scope.startWatchEmail();
            $scope.model = {};
            $scope.model.role = ROLES.NETWORKUSER;
            $scope.editModal.$promise.then(function () {
                $scope.editModal.show();
            }.bind(this));
        };

        $scope.edit = function ($event, id) {
            $event.stopPropagation();

            $scope.setModelById(id);
            $scope.editModal.$promise.then(function () {
                $scope.editModal.show();
            }.bind(this));
        };

        $scope.suspend = function ($event, id) {
            $event.stopPropagation();

            rest.path = 'user/suspend/' + id;
            rest.put().success(function (data) {
                toaster.pop('success', data.result.text);
                $scope.models.forEach(function (model) {
                    if (model.id == id) {
                        model.status = data.result.status;
                    }
                });
            }).error(callback.error);
        }

        $scope.remove = function (id) {
            rest.path = 'user/' + id;
            rest.deleteModel().success(function (data) {
                $scope.models.forEach(function (model) {
                    if (model.id == id) {
                        $scope.models.splice($scope.models.indexOf(model), 1);
                    }
                });
            }).error(function (data) {
                callback.error(data);
            });
        };

        $scope.save = function () {
            $scope.inProgress = true;
            var fd = new FormData();
            $scope.setDataToForm(fd, $scope.model)
            fd.append("image", $scope.model.image);

            if ($scope.model.id) {
                rest.path = 'user/' + $scope.model.id;
                rest.fileModel(fd).success(function (data) {
                    $scope.model = {};
                    $scope.editModal.hide();
                    $scope.search(); //get(update) users from server
                }).error(function (data) {
                    callback.error(data);
                }).finally(function () {
                    $scope.inProgress = false;
                });
            } else {
                rest.path = 'user';
                rest.fileModel(fd).success(function (data) {
                    $scope.model = {};
                    $scope.editModal.hide();
                    $scope.search();
                }).error(function (data) {
                    callback.error(data);
                }).finally(function () {
                    $scope.inProgress = false;
                });
            }
        };

        $scope.setDataToForm = function (formData, data) {
            function set(key, value) {
                if (typeof (value) == "object") {
                    for (var i in value) {
                        set(key + '[' + i + ']', value[i]);
                    }
                } else {
                    formData.append(key, value);
                }
            }

            for (var key in data) {
                set(key, data[key]);
            }
        }

        $scope.clear = function () {
            $scope.searchmodel = {}; //search fields
            $scope.search(); //search all
        }
    }
]
);
