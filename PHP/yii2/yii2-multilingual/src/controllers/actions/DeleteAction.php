<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\controllers\actions;

use gbksoft\multilingual\models\Language;
use yii\base\Action;
use yii\web\NotFoundHttpException;
use yii\web\Response;

/**
 * Class DeleteAction
 */
class DeleteAction extends Action
{
    /**
     * @param int $id
     * @return Response
     * @throws NotFoundHttpException
     */
    public function run($id)
    {
        /** @var Language $model */
        $model = Language::findOne($id);
        if ($model === null) {
            throw new NotFoundHttpException('The requested language does not exist.');
        }

        $model->delete();

        return $this->controller->redirect(['view']);
    }
}
