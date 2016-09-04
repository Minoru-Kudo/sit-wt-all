/**
 *
 */
function init(){

	var modeHelp = "スクリーンショット比較時における除外箇所を指定するモードです。";
	modeHelp += "スクリーンショット上に配置した付箋の形にマスク処理がされます。"
	var fusenHelp = "マスクをかけたい箇所にドラッグします。";
	var trashBoxHelp = "削除したい付箋はここにドロップします。";
	var loadHelp = "指定したJSONファイルを読み込み、付箋の配置を復元します。";
	var saveHelp = "スクリーンショット上に配置した付箋の情報をJSON形式で保存します。";
	var deleteHelp = "配置済みの付箋をすべて削除します。";

	$("body >:first").after("<div id='modeSelect'></div>");
	$("#modeSelect").append("<p><button id='switch'>切替</button> 付箋モード<span class='tooltip' title='" + modeHelp + "'>[?]</span>：<span id='mode'>OFF</span></p>");
	$("#modeSelect").append("<hr/>");

	$("#modeSelect").after("<div id='toolBox'></div>");
	$("#toolBox").append("付箋<span class='tooltip' title='" + fusenHelp + "'>[?]</span><br/>");
	$("#toolBox").append("<div id='initialFusenPos'></div>");
	$("#toolBox").append("<br/>");
	$("#toolBox").append("<br/>");
	$("#toolBox").append("<br/>");
	$("#toolBox").append("<br/>");
	$("#toolBox").append("ゴミ箱<span class='tooltip' title='" + trashBoxHelp + "'>[?]</span><br/>");
	$("#toolBox").append("<div id='trashBox'></div>");
	$("#toolBox").append("<br/>");
	$("#toolBox").append("<button id='load'>読込</button><span class='tooltip' title='" + loadHelp + "'>[?]</span>");
	$("#toolBox").append("<button id='save'>保存</button><span class='tooltip' title='" + saveHelp + "'>[?]</span> ");
	$("#toolBox").append("<button id='delete' disabled='disabled'>削除</button><span class='tooltip' title='" + deleteHelp + "'>[?]</span>");
	$("#toolBox").after("<div id='movedFusen'></div>");

	$(".tooltip").tooltip();

}

// 付箋処理
$(function() {

	init();

	var initialFusenPos = $("#initialFusenPos");
	var movedFusen = $("#movedFusen");

	var createFusen = function() {

		initialFusenPos.append("<div class='fusen'></div>");
		var fusen = initialFusenPos.find("div:last");

		fusen.resizable().draggable()
		.on("drag", function(event, ui) {

			if (isOut(fusen, initialFusenPos)) {
				createFusen();
				$(this).css("position", "absolute").off("drag"); // .fusenはposition:relativeのため、ここでabsoluteにして新規付箋をfusenBaseに配置可能にする
			}

		}).on("dragstop", function(event, ui) {

			if ($(this).parent().attr("id") == "initialFusenPos") {
				$(this).css({
					left : ui.position.left + initialFusenPos.offset().left + "px",
					top : ui.position.top + initialFusenPos.offset().top + "px"
				}).appendTo(movedFusen);
			}
			$("#delete").removeAttr("disabled");

		});
	};

	var isOut = function(target, base) {

		var targetPos = target.offset();
		var basePos = base.offset();

		if (targetPos.left < basePos.left ||
			targetPos.left > basePos.left + base.width() ||
			targetPos.top < basePos.top ||
			targetPos.top > basePos.top + base.height()) {
			return true;
		}

		return false;

	};

	// JSONデータ作成
	var writeToJson = function(images, fusenArray) {

		var jsonData = {};

		jsonData.evidenceName = window.location.href.split('/').pop();
		jsonData.maskInfo = buildMaskInfoArray(images, fusenArray);

		return JSON.stringify(jsonData, undefined, "\t");

	};

	var buildMaskInfoArray = function(images, fusenArray){

		var maskInfoArray = [];

		images.each(function() {

			var maskInfo = {};
			var img = $(this);

			maskInfo.imgSrc = img.attr('src');
//			maskInfo.imgSrc = img.attr('src').replace("/", "\\/"); // ← 置換後の文字列が"\\/"となるため、Javaで置換
			maskInfo.posStyle = buildPosStyleArray(fusenArray, img);
			maskInfoArray.push(maskInfo);

		});

		return maskInfoArray;
	};

	var buildPosStyleArray = function(fusenArray, img){

		var posStyleArray = [];
		var imgPos = img.offset();

		fusenArray.each(function() {

			var posStyle = {};
			var fusen = $(this);
			var fusenPos = fusen.offset();

			if (!isOut(fusen, img)) {
				posStyle.x = Math.ceil(fusenPos.left - imgPos.left);
				posStyle.y = Math.ceil(fusenPos.top - imgPos.top);
				posStyle.width = Math.ceil(fusen.width());
				posStyle.height = Math.ceil(fusen.height());

				posStyleArray.push(posStyle);
			}

		});

		return posStyleArray;
	};


	// ファイルダウンロード
	var save = function(data, fileName) {

		var blob = new Blob([ data ], {
			"type" : "application/x-msdownload"
		});

		// for IE / Edge
		if (window.navigator.msSaveBlob) {
			window.navigator.msSaveBlob(blob, fileName);

		// for others
		} else {

			var link = document.createElement("a");
			document.body.appendChild(link);
			link.href = (window.URL || window.webkitURL).createObjectURL(blob);
			link.download = fileName;
			link.click();
			document.body.removeChild(link);

		}
	};

	// inputタグ生成
	var createInput = function() {

		$("#toolBox").append("<input id='file' type='file' style='display: none;'>");

		// ファイル読込
		$("#file").change(function() {

			var input = $(this);
			var reader = new FileReader();
			reader.readAsText($(this)[0].files[0]);
			reader.onload = function() {

				// jsonチェック（違うエビデンスのjsonならエラーメッセージを表示し、何もしない）
				if(checkFile(input.val().split("\\").pop())){
					if(movedFusen.children().length > 0){
						confirmFusenRestore(reader.result);
					} else {
						relocateFusen(reader.result);
					}
					input.remove();
					createInput();
				}
//				if(checkFile(input.val().split("\\").pop())){
//					var msg = "配置済みの付箋を削除してから付箋の配置を復元します。よろしいですか？";
//					if(movedFusen.children().length > 0 && confirm(msg)){ // 「Confirm」ダイアログではOK, キャンセルでしか聞かれないため jQueryUIで実装
//						movedFusen.children().remove();
//					}
//					relocateFusen(reader.result);
//				}
//
//				input.remove();
//				createInput();
			};
		});
	};

	var checkFile = function(fileName) {

		var evidenceName = window.location.href.split("/").pop();

		if (fileName.startsWith(evidenceName) && /.json$/i.test(fileName) ) {
			return true;
		} else {
			alert("このエビデンスに合致しないファイルです。ファイルを選択し直してください。");
		}
		return false;
	};

	var confirmFusenRestore = function(text) {

		// ダイアログの作成
		var msg = "このエビデンス上に配置済みの付箋があります。該当する操作をクリックしてください。";
		$("#toolBox").append("<div id='fusenRestoreDialog' title='付箋配置の復元'>" + msg + "</div>");

		$("#fusenRestoreDialog").dialog({
			autoOpen: false,
			modal: true,
			width: 900,
			buttons: [
				{
					text: "配置済みの付箋を削除してから復元する",
					click: function(){
						movedFusen.children().remove();
						relocateFusen(text);
						$(this).dialog("close");
					}
				},
				{
					text: "配置済みの付箋を残したまま復元する",
					click: function(){
						relocateFusen(text);
						$(this).dialog("close");
					}
				},
				{
					text: "キャンセル",
					click: function(){
						$(this).dialog("close");
					}
				}
			]
		});

		// ダイアログ表示
		$("#fusenRestoreDialog").dialog("open");

	};

	// 付箋再配置処理
	var relocateFusen = function(data) {

		var maskInfoArray = $.parseJSON(data).maskInfo;

		jQuery.each(maskInfoArray, function(i, maskInfo) {

			var img = $("img[src='" + maskInfo.imgSrc + "']");
			var posStyleArray = maskInfo.posStyle;

			jQuery.each(posStyleArray, function(j, posStyle) {

				createFusen();
				var fusen = initialFusenPos.find(".fusen:last").appendTo(movedFusen)
				.off("drag").css("position", "absolute");

				fusen.css({"left" : img.offset().left + posStyle.x,
					"top" : img.offset().top + posStyle.y,
					"width" : posStyle.width,
					"height" : posStyle.height});
			});
		});

		$("#delete").removeAttr("disabled");

	};

	var confirmFusenDelete = function() {

		// ダイアログの作成
		var msg = "配置済みの付箋をすべて削除しますか？";
		$("#toolBox").append("<div id='fusenDeleteDialog' title='付箋の削除'>" + msg + "</div>");

		$("#fusenDeleteDialog").dialog({
			autoOpen: false,
			modal: true,
			width: 500,
			buttons: [
				{
					text: "はい",
					click: function(){
						movedFusen.children().remove();
						$("#delete").attr("disabled", "disabled");
						$(this).dialog("close");
					}
				},
				{
					text: "いいえ",
					click: function(){
						$(this).dialog("close");
					}
				}
			]
		});

		// ダイアログ表示
		$("#fusenDeleteDialog").dialog("open");

	};

	// 初期付箋生成
	createFusen();

	// 読込ボタン生成
	createInput();

	$("#trashBox").droppable().on("drop", function(event, ui) {
		ui.helper.context.remove();
		if (movedFusen.children().length == 0) {
			$("#delete").attr("disabled", "disabled");
		}
	});

	// 初期表示では表示しない
	$('#toolBox').toggle(false);

	$("#switch").click(function() {
		$('#toolBox').toggle();
		$('#movedFusen > .fusen').toggle();
		if ($("#mode").text() == "OFF") {
			$("#mode").text("ON");
		} else {
			$("#mode").text("OFF");
		}
	});

	$("#load").click(function() {
		$("#file").click();
	});

	$("#save").click(function() {
		var jsonName = window.location.href.split("/").pop() + ".json";
		save(writeToJson($("img"), movedFusen.children()), jsonName);
	});

	$("#delete").click(function() {
		confirmFusenDelete();
	});

});

//キー操作で表示非表示切り替え
$(window).keydown(function(e){
	if (e.keyCode == 70) { // Key[F]
		$('#toolBox').toggle();
		$('#movedFusen > .fusen').toggle();
		if ($("#mode").text() == "OFF") {
			$("#mode").text("ON");
		} else {
			$("#mode").text("OFF");
		}
	}
});