/**
 * Image edit tools
 *
 * @author GBKSOFT team <hello@gbksoft.com>
 * @link http://gbksoft.com
 * @copyright 2011-2015 GBKSOFT team
 * @since 1.0
 *
 */

var editor;

$(document).ready(function () {
    editor = new Editor();
});

function Editor() {
    var self = this;
    this.folder = null;
    this.path = null;
    this.path_temp = null;
    this.img = new Image();
    this.mask = null;
    this.resultImg = null;
    this.canvas = null;
    this.backcanvas = null;
    this.action = null;
    this.coef_size = 1;
    this.Editor = function () {
        this.setImage(function () {
            self.resizeImage();
            self.canvas = new Canvas(self.img);
            self.mask = new Mask(self.path_temp);
            self.backcanvas = new Backcanvas(self.img);
            self.backcanvas.context.drawImage(self.img, self.backcanvas.margin, self.backcanvas.margin, self.img.width, self.img.height);
            self.setEvents();
            self.setIframeSize();
        });

    };

    this.setIframeSize = function () {
        var dialog_editor = window.parent.workspace.dialog_editor;
        window.parent.$(dialog_editor).dialog('option', 'width', self.canvas.element.width + 35);
        window.parent.$(dialog_editor).dialog('option', 'height', self.canvas.element.height + 115);
        window.parent.$(dialog_editor).find('iframe').attr({
            'height': self.canvas.element.height + 50,
            'width': self.canvas.element.width
        });
    };

    this.setImage = function (onload) {
        this.user_folder = $('input[name=user_folder]').val();
        this.path_temp = base_url + 'images/' + this.user_folder + '/temp/';
        this.folder = $('input[name=folder]').val();
        this.img.name = $('input[name=name]').val();
        this.img.src = $('input[name=file]').val();
        this.img.onload = onload;
    };

    this.resizeImage = function () {
        var workspace = window.parent.workspace;
        var workspace_size = workspace.getSize();
        if (self.img.height > workspace_size.height) {
            self.coef_size = ((workspace_size.height / 100) * 80) / self.img.height;
            self.img.height = self.img.height * self.coef_size;
            self.img.width = self.img.width * self.coef_size;
        }
        if (self.img.width > workspace_size.width) {
            self.coef_size = ((workspace_size.width / 100) * 80) / self.img.width;
            self.img.height = self.img.height * self.coef_size;
            self.img.width = self.img.width * self.coef_size;
        }
    };

    this.setEvents = function () {
        $('input[name=magnetic]').click(self.magnetic);
        $('input[name=cut]').click(self.cut);
        $('input[name=crop]').click(self.crop);
        $('input[name=reset]').click(self.reset);
        $('input[name=save]').click(self.save);
    };

    this.toPostData = function () {
        this.canvas.coord.points = [];
        for (var i = 0; i < this.mask.maskPath.length; i++) {
            this.canvas.coord.set({
                'x': (this.mask.maskPath[i].x - this.canvas.margin) / this.coef_size,
                'y': (this.mask.maskPath[i].y - this.canvas.margin) / this.coef_size
            });
        }
    };

    this.toPostCoords = function (coords) {
        var points = [];
        for (var i = 0; i < coords.length; i++) {
            points.push({
                'x': (coords[i].x - this.canvas.margin) / this.coef_size,
                'y': (coords[i].y - this.canvas.margin) / this.coef_size
            });
        }
        return points;
    };

    this.fromPostData = function (points) {
        self.mask.maskPath = [];
        for (var i = 0; i < points.length; i++) {
            self.mask.maskPath.push({
                'x': (points[i].x + self.canvas.margin) * self.coef_size,
                'y': (points[i].y + self.canvas.margin) * self.coef_size
            });
        }
    };

    this.get_folder = function () {
        var path = '';
        if (self.img.src.indexOf('/wishlist/') != -1) {
            path += '/uploads/wishlist/';
        } else if (self.img.src.indexOf('/closet/') != -1) {
            path += '/uploads/closet/';
        }
        path += self.folder;

        return path;
    };

    this.magnetic = function () {
        if (self.mask.maskPath.length > 0) {
            self.toPostData();
            $('#loader').show();
            $.post(base_url + 'imageedit/index/', {
                'data': JSON.stringify(self.canvas.coord),
                'file': self.img.name,
                'folder': self.get_folder(),
                'action': 'crop'
            }, function (response) {
                if (response) {
                    self.canvas.coord.points = [];
                    self.fromPostData(response.points);
                    self.mask.maskPath = expandPoly(self.mask.maskPath, -3);
                    self.action = 'destination-in';
                    self.canvas.draw.frame();
                }
            }, 'json');
        }
        this.action = null;
    };

    this.reset = function () {
        self.mask.maskPath = [];
        self.canvas.polygon = null;
        self.canvas.clear();
        self.canvas.coord.points = [];
        self.canvas.coord.crops = [];
        self.canvas.coord.cuts = [];
        self.backcanvas.context.drawImage(self.img, self.backcanvas.margin, self.backcanvas.margin, self.img.width, self.img.height);
    };

    this.cut = function () {
        if (self.canvas.polygon) {
            self.canvas.coord.setCut(self.toPostCoords(self.mask.maskPath));
            self.action = 'destination-out';
            self.backcanvas.draw.mask();
            self.canvas.polygon = null;
            self.mask.maskPath = [];
            $(self.canvas.element).unbind('mousemove');
            $(self.canvas.element).unbind('mouseup');
        } else {
            alert('No selection');
        }
    };

    this.crop = function () {
        if (self.canvas.polygon) {
            self.canvas.coord.setCrop(self.toPostCoords(self.mask.maskPath));
            self.action = 'destination-in';
            self.backcanvas.draw.maskCrop();
            self.canvas.polygon = null;
            self.mask.maskPath = [];
            $(self.canvas.element).unbind('mousemove');
            $(self.canvas.element).unbind('mouseup');
        } else {
            alert('No selection');
        }
    };

    this.save = function () {
        var workspace = window.parent.workspace;
        var settings = workspace.temp.currentSettings;
        if (settings.folder == 'default') {
            $.blockUI({
                message: $('#image_types_view'),
                css: {
                    textAlign: 'left',
                    width: 'auto',
                    zIndex: 10000,
                    background: 'url("images/ui-bg_highlight-soft_75_cccccc_1x100.png") repeat-x scroll 50% 50% #CCCCCC',
                    padding: '15px',
                    borderRadius: '10px',
                    top: '10%',
                    left: '10%'
                }
            });
            $('#image_types_view').find('#image_type_button_ok').click(function () {
                var image_type = $('form.image_types').serializeArray();
                settings.folder = image_type[0].value;
                settings.type = image_type[1].value;
                $.unblockUI({
                    onUnblock: function () {
                        $.blockUI({
                            css: {
                                border: 'none',
                                padding: '15px',
                                backgroundColor: '#000',
                                '-webkit-border-radius': '10px',
                                '-moz-border-radius': '10px',
                                opacity: .5,
                                color: '#fff'
                            }
                        });
                        window.parent.server.cutcrop(self.img.src, JSON.stringify({
                            'crop': self.canvas.coord.crops,
                            'cut': self.canvas.coord.cuts,
                            'folder': settings.folder,
                            'type': settings.type
                        }), function (data) {

                            console.log('srcer', data);
                            window.parent.libraries.reloadImages();

                            settings.src = data.url + '?' + Math.random();
                            var coef_size = settings.height / data.fullsize.height;
                            settings.width = data.width * coef_size;
                            settings.height = data.height * coef_size;
                            workspace.removeGroup();
                            var canvas = workspace.add(settings.src, settings.center);
                            canvas.restore(settings, function () {
                            });
                            workspace.closeDialog();
                        });
                    }
                });
                $('#image_types_view').find('#image_type_button_ok').unbind('click');
            });
            $('#image_types_view').find('#image_type_button_cancel').click(function () {
                $.unblockUI($('#fileToUpload').val(''));
                $('#image_types_view').find('#image_type_button_cancel').unbind('click');
            });

        } else {
            window.parent.server.cutcrop(self.img.src, JSON.stringify({
                'crop': self.canvas.coord.crops,
                'cut': self.canvas.coord.cuts,
                'folder': settings.folder,
                'type': settings.type
            }), function (data) {

                window.parent.libraries.reloadImages();

                settings.src = data.url + '?' + Math.random();
                var coef_size = settings.height / data.fullsize.height;
                settings.width = data.width * coef_size;
                settings.height = data.height * coef_size;
                workspace.removeGroup();
                var canvas = workspace.add(settings.src, settings.center);
                canvas.restore(settings, function () {
                });
                workspace.closeDialog();
            });
        }
    };

    this.addPoint = function (e, flag) {
        var prev_point = null;
        var tempArray = [];
        var inline = 0;
        for (var k = 0; k <= self.mask.maskPath.length; k++) {
            if (self.mask.maskPath.length > 0) {
                prev_point = self.mask.maskPath[k - 1] ? self.mask.maskPath[k - 1] : self.mask.maskPath[k];
                var next_point = typeof (self.mask.maskPath[k]) != 'undefined' ? self.mask.maskPath[k] : self.mask.maskPath[0];
                var point = {
                    'x': e.pageX,
                    'y': e.pageY
                };
                var l = self.mask.nearestPointOnVector({
                    'x': prev_point.x,
                    'y': prev_point.y
                }, {
                    'x': next_point.x,
                    'y': next_point.y
                }, point, 5);
                if (l != false && self.mask.nearestPoint(point, self.canvas.draw.point_size) < 0) {
                    inline = 1;
                    if (flag) {
                        tempArray.push({
                            'x': e.pageX,
                            'y': e.pageY
                        });
                    }
                }
                if (typeof (self.mask.maskPath[k]) != 'undefined') {
                    //self.canvas.coord.set();
                    tempArray.push(self.mask.maskPath[k]);
                }
            }
        }
        self.mask.maskPath = tempArray;
        self.canvas.draw.frame();
        if (!flag && inline) {
            self.canvas.draw.point(point, self.canvas.draw.point_size, 'blue');
        }
    };

    this.Editor();
}

function Backcanvas(img) {
    var self = this;
    this.element = null;
    this.context = null;
    this.margin = 50;
    this.draw = null;
    this.Backcanvas = function (img) {
        this.element = document.createElement('canvas');
        if ($.browser.msie && false) {
            G_vmlCanvasManager.initElement(this.element);
        }
        $('#hiden_div').append(this.element);
        this.context = this.element.getContext('2d');
        this.element.width = img.width + self.margin * 2;
        this.element.height = img.height + self.margin * 2;
        this.draw = new Draw(self);
    };

    this.clear = function () {
        self.context.clearRect(0, 0, self.element.width, self.element.height);
    };

    this.Backcanvas(img);
}

function Canvas(img) {
    var self = this;
    this.element = null;
    this.context = null;
    this.margin = 50;
    this.polygon = null;
    this.draw = null;
    this.data = null;
    this.coord = {
        start: {
            'x': null,
            'y': null
        },
        min: {
            'x': null,
            'y': null
        },
        max: {
            'x': null,
            'y': null
        },
        crops: [],
        cuts: [],
        points: [],
        set: function (point) {
            point = {
                'x': point.x,
                'y': point.y
            };
            this.points.push(point);
            this.min.x = (!this.min.x) || (this.min.x > point.x) ? point.x : this.min.x;
            this.max.x = (!this.max.x) || (this.max.x < point.x) ? point.x : this.max.x;
            this.min.y = (!this.min.y) || (this.min.y > point.y) ? point.y : this.min.y;
            this.max.y = (!this.max.y) || (this.max.y < point.y) ? point.y : this.max.y;
        },
        setCrop: function (points) {
            this.crops.push(points);
        },
        setCut: function (points) {
            this.cuts.push(points);
        }
    };
    this.position = null;
    this.Canvas = function (img) {
        this.element = document.createElement('canvas');
        if ($.browser.msie && false) {
            G_vmlCanvasManager.initElement(this.element);
        }
        $('#canvas_div').append(this.element);
        this.context = this.element.getContext('2d');
        this.element.width = img.width + self.margin * 2;
        this.element.height = img.height + self.margin * 2;
        $('.canvas_container').css('width', this.element.width + 'px');
        $('.canvas_container').css('height', this.element.height + 'px');
        //$('.canvas_background').css('width',this.element.width+'px');
        //$('.canvas_background').css('height',this.element.height+'px');
        this.setPosition();
        this.setEvents();
        this.draw = new Draw(self);

    };

    this.setPosition = function () {
        var pos = $(this.element).position();
        this.position = {
            'x': pos.left,
            'y': pos.top
        };
    };

    this.setEvents = function () {
        $(this.element).mousedown(function (e) {
            if (self.polygon) {
                editor.addPoint(e, true);
                $(self.element).mousemove(function (e) {
                    if (editor.mask.maskPath.length > 0)
                    {
                        editor.mask.moveNearestsTo({
                            'x': e.pageX - self.position.x,
                            'y': e.pageY - self.position.y
                        });
                        self.draw.frame();
                    }

                });
                $(self.element).mouseup(function (e) {
                    editor.mask.movedPointIndex = -1;
                    $(self.element).unbind('mousemove');
                    $(self.element).unbind('mouseup');
                    $(self.element).mousemove(function (e) {
                        editor.addPoint(e, false);
                    });
                });
                $(self.element).mousemove(function (e) {
                    editor.addPoint(e, false);
                });
            } else {
                var point = {
                    'x': e.pageX,
                    'y': e.pageY
                };
                var start_point = self.coord.points[0];
                if (start_point && point.x > start_point.x - self.draw.point_size / 2 && point.x < start_point.x + self.draw.point_size / 2 && point.y > start_point.y - self.draw.point_size / 2 && point.y < start_point.y + self.draw.point_size / 2) {
                    self.coord.points = [];
                    self.polygon = true;
                    self.draw.frame();
                    $(self.element).unbind('mousemove');
                    $(self.element).unbind('mouseup');
                    return true;
                }
                self.coord.set({
                    'x': point.x,
                    'y': point.y
                });
                editor.mask.maskPath.push({
                    'x': point.x,
                    'y': point.y
                });
                var prev_point = self.coord.points[self.coord.points.length - 2] ? self.coord.points[self.coord.points.length - 2] : point;
                self.draw.line(prev_point, point, 1, '#000000');
                self.draw.point(point, self.draw.point_size, 'black');
                var imageData = self.context.getImageData(self.position.x, self.position.y, self.element.width, self.element.height);
                $(self.element).mousemove(function (e) {
                    var cursor = {
                        'x': e.pageX - self.position.x,
                        'y': e.pageY - self.position.y
                    };
                    self.clear();
                    self.context.putImageData(imageData, self.position.x, self.position.y);
                    self.draw.line(point, cursor, 1, '#000000');
                });
            }
        });
        $(this.element).dblclick(function () {
            self.clear();
            editor.mask.maskPath = [];
            self.polygon = null;
            self.coord.points = [];
            $(self.element).unbind('mousemove');
            $(self.element).unbind('mouseup');
        });
    };

    this.clear = function () {
        self.context.clearRect(0, 0, self.element.width, self.element.height);
    };

    this.Canvas(img);
}

function Draw(canvas) {
    this.point_size = 9;
    this.Draw = function () {
    };

    this.line = function (start, point, width, color) {
        canvas.context.lineWidth = width;
        canvas.context.strokeStyle = color;
        canvas.context.beginPath();
        canvas.context.moveTo(start.x, start.y);
        canvas.coord.start = point;
        canvas.context.lineTo(point.x, point.y);
        canvas.context.stroke();
        canvas.context.closePath();
    };

    this.maskCrop = function () {
        editor.canvas.clear();
        var src = canvas.element.toDataURL();
        canvas.clear();
        canvas.context.save();
        canvas.draw.poly(editor.mask.maskPath);
        canvas.context.clip();
        var image = new Image();
        $(image).load(function () {
            canvas.context.drawImage(image, 0, 0);
            canvas.context.restore();
        });
        image.src = src;

        $('#loader').hide();
    };

    this.mask = function () {
        editor.canvas.clear();
        canvas.context.save();
        canvas.context.globalCompositeOperation = editor.action;
        canvas.draw.poly(editor.mask.maskPath, 'fill');
        canvas.context.globalCompositeOperation = 'source-over';
        canvas.context.restore();
        canvas.draw.poly(editor.mask.maskPath);
        $('#loader').hide();
    };

    this.poly = function (points, command) {
        canvas.context.beginPath();
        canvas.context.moveTo(points[0].x, points[0].y);
        for (var i = 0; i < points.length; ++i) {
            canvas.context.lineTo(points[i].x, points[i].y);
        }
        canvas.context.moveTo(points[0].x, points[0].y);
        canvas.context.lineTo(points[points.length - 1].x, points[points.length - 1].y);
        switch (command) {
            case 'line':
                canvas.context.lineWidth = 1;
                canvas.context.strokeStyle = '#000000';
                canvas.context.stroke();
                break;
            case 'fill':
                canvas.context.fillStyle = '#00D2FF';
                canvas.context.fill();
                break;
            default:
                break;
        }
        canvas.context.closePath();
    };

    this.frame = function () {
        canvas.clear();
        canvas.context.save();
        canvas.draw.poly(editor.mask.maskPath, 'line');
        canvas.context.restore();
        for (var i = 0; i < editor.mask.maskPath.length; i++) {
            canvas.draw.point({
                'x': editor.mask.maskPath[i].x,
                'y': editor.mask.maskPath[i].y
            }, this.point_size, 'black');
        }
        $('#loader').hide();
    };

    this.maskWithPoly = function () {
        //$('.canvas_background').css('opacity', '0.5');
        canvas.clear();
        canvas.context.save();
        canvas.context.drawImage(editor.img, canvas.margin, canvas.margin, editor.img.width, editor.img.height);
        canvas.context.globalCompositeOperation = editor.action;
        canvas.draw.poly(editor.mask.maskPath, 'fill');
        canvas.context.globalCompositeOperation = 'source-over';
        canvas.context.restore();
        canvas.draw.poly(editor.mask.maskPath, 'line');

        for (var i = 0; i < editor.mask.maskPath.length; i++) {
            canvas.draw.point({
                'x': editor.mask.maskPath[i].x,
                'y': editor.mask.maskPath[i].y
            }, this.point_size, 'black');
        }
        $('#loader').hide();
    };

    this.point = function (point, width, color) {
        canvas.context.beginPath();
        canvas.context.arc(point.x, point.y, width / 2, 0, 2 * Math.PI, false);
        canvas.context.fillStyle = '#FFFFFF';
        canvas.context.fill();
        canvas.context.lineWidth = 1;
        canvas.context.strokeStyle = '#000000';
        canvas.context.stroke();
        canvas.context.closePath();
    };

    this.Draw();
}
function Mask(path) {
    var self = this;
    this.src = null;
    this.path = null;
    this.image = null;
    this.onload = function () {
    };
    this.maskPath = [];
    this.Mask = function (path) {
        this.path = path;
    };
    this.movedPointIndex = -1;
    this.rotate = 0;

    this.vectorLength = function (p1, p2) {
        return Math.sqrt(Math.abs(Math.pow(p2.x - p1.x, 2)) + Math.abs(Math.pow(p2.y - p1.y, 2)));
    };

    this.pointOnVector = function (p1, p2, length) {
        var g = this.vectorLength(p1, p2);
        var c = p2.x - p1.x;
        var sin = c / g;
        var c1 = length * sin;
        var c2 = length * (1 / sin);
        return {
            'x': p1.x + c1,
            'y': p1.y + c2
        };
    };

    this.nearestPoint = function (point, precision)
    {
        precision = (typeof (precision) == 'undefined') ? 5 : precision;
        var index = -1;
        var min = 10000;

        for (var i = 0; i < this.maskPath.length; i++)
        {
            var start = this.maskPath[i];
            var l = this.vectorLength(start, point);
            if (l < min)
            {
                min = l;
                index = i;
            }
        }
        this.movedPointIndex = min <= precision ? index : -1;
        return this.movedPointIndex;
    };

    this.moveNearestsTo = function (point)
    {
        var index = this.movedPointIndex >= 0 ? this.movedPointIndex : this.nearestPoint(point);
        if (index >= 0)
        {
            var diff = point - this.maskPath[index];
            var sp = this.maskPath[index];
            if (index > 0)
            {
                var cp = this.maskPath[index - 1];
            }
            if (index + 1 < this.maskPath.length)
            {
                var cp = this.maskPath[index + 1];
            }
            this.maskPath[index] = point;
            return true;
        }
        return false;
    };

    this.nearestPointOnVector = function (p1, p2, p3, length) {
        var x1 = p2['x'] - p1['x'];
        var y1 = p2['y'] - p1['y'];
        var x2 = p3['x'] - p1['x'];
        var y2 = p3['y'] - p1['y'];
        var cos = (x1 * x2 + y1 * y2) / Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
        var sin = Math.sin(Math.acos(cos));
        var b = Math.sqrt(Math.abs(Math.pow(x1, 2)) + Math.abs(Math.pow(y1, 2)));
        var c = Math.sqrt(Math.abs(Math.pow(x2, 2)) + Math.abs(Math.pow(y2, 2)));
        if (b >= c * cos && c * sin < length && c * cos > 0) {
            return this.pointOnVector(p1, p2, c * cos);
        }
        return false;
    };

    this.Mask(path);
}